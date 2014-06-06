package com.mcdermotsoft.glitchenjournal;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

@SuppressWarnings("serial")
public class DatabaseTask extends HttpServlet 
{
	int numAuctionsScanned, numAuctionsDeleted, numMemcacheRead;
	JSONObject auctionHouse;
	ArrayList<Entity> auctionEntities, averageEntities, currentAuctions;
	static ArrayList<Entity> previousAuctions = new ArrayList<Entity>();
	MemcacheService memcache;
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	Queue queue = QueueFactory.getDefaultQueue();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		numAuctionsScanned = 0; 
		numAuctionsDeleted = 0;
		numMemcacheRead = 0;
		auctionEntities = new ArrayList<Entity>();
		averageEntities = new ArrayList<Entity>();
		currentAuctions = new ArrayList<Entity>();
		memcache = MemcacheServiceFactory.getMemcacheService();
		
		try
		{
			getAuctions(1);
			
			datastore.put(averageEntities);
			datastore.put(auctionEntities);
			
			purgeSoldEntities();
			purgeExpiredEntities();
			System.out.println("Auctions scanned: " + numAuctionsScanned + ", Added: " + (auctionEntities.size() + averageEntities.size()) + " Memcache gets: " + numMemcacheRead + ", Deletes: " + numAuctionsDeleted);
		} 
		catch (JSONException ex) 
		{
			ex.printStackTrace();
		}
		catch(IOException ex)
		{
			System.err.println("could not fetch auctions");
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println("done");
		resp.getWriter().println("Auctions scanned: " + numAuctionsScanned + ", Added: " + (auctionEntities.size() + averageEntities.size()) + " Memcache gets: " + numMemcacheRead + ", Deletes: " + numAuctionsDeleted);
	}
	
	private void getAuctions(int page) throws JSONException, IOException
	{
		String response = "";
		try 
		{
			URL url = new URL("http://api.glitch.com/simple/auctions.list?defs=1&per_page=500&page=" + page);
			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));
			response = in.readLine();
			in.close();
			
			JSONTokener tokener = new JSONTokener(response);
			auctionHouse = new JSONObject(tokener);
			
			if(auctionHouse == null)
				throw new IOException();
			else
			{
				int maxPage = auctionHouse.getInt("pages");
				if(page == 1)
				{
					for(int i=2; i<= maxPage; i++)
					{
						
					}
				}
				if(page <= maxPage)
				{
					updateDatabase();
					page++;
					getAuctions(page);
				}
			}
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
		}
	}
	
	private void updateDatabase() throws JSONException
	{			
		JSONObject auctions = auctionHouse.optJSONObject("items");
		JSONArray items = auctions.names();
				
		for(int i=0; i<items.length(); i++)
		{
			numAuctionsScanned++;
			
			String auctionID = items.getString(i);
			String firstHalf = auctionID.substring(auctionID.indexOf("-")+1);
			String secondHalf = auctionID.substring(0, auctionID.indexOf("-"));
			auctionID = firstHalf + "-" + secondHalf;
			
			Entity en = (Entity) memcache.get(auctionID);
			if(en != null)
			{
				numMemcacheRead++;
				currentAuctions.add(en);
				continue;
			}
			else
			{
				Key k = KeyFactory.createKey("Auctions", auctionID);
				try 
				{
					Entity temp = datastore.get(k);
					memcache.put(auctionID, temp);
					currentAuctions.add(temp);
					continue;
				} 
				catch(EntityNotFoundException ex) 
				{
					//new auction - proceed to store it
				}
			}
			
			JSONObject item = auctions.getJSONObject(items.getString(i));
			JSONObject itemDef = item.getJSONObject("item_def");
			JSONObject player = item.getJSONObject("player");
			
			String name = itemDef.optString("name_single");
			String iconUrl = itemDef.optString("iconic_url");
			String costS = item.optString("cost");
			String category = item.optString("category");
			String class_tsid = item.optString("class_tsid");
			String countS = item.optString("count");
			String playerName = player.optString("name");
			int count = Integer.valueOf(countS);
			double cost = Float.valueOf(costS);
			long seen = 0;
			double costHistory = 0;
						
			Entity average = (Entity) memcache.get(class_tsid);
			if(average != null)
			{
				numMemcacheRead++;
				seen = (Long) average.getProperty("seen");
				costHistory = (Double) average.getProperty("cost");
			}
			else
			{
				Key k = KeyFactory.createKey("Averages", class_tsid);
				try
				{
					Entity temp = datastore.get(k);
					seen = (Long) temp.getProperty("seen");
					costHistory = (Double) temp.getProperty("cost");
				}
				catch(EntityNotFoundException ex)
				{
					//new item - costHistory and seen are 0
					queue.add(withUrl("/task/getimagetask").param("url", iconUrl).param("tsid", class_tsid).method(Method.GET));
				}
			}
			
			double weightedCost = (costHistory*seen + cost)/(seen+count);
			
			Entity historyEntity = new Entity("Averages", class_tsid);
			historyEntity.setUnindexedProperty("category", category);
			historyEntity.setProperty("name", name.toLowerCase());
			historyEntity.setUnindexedProperty("cost", weightedCost);
			historyEntity.setUnindexedProperty("seen", seen+count);
			
			Entity auctionsEntity = new Entity("Auctions", auctionID);
			auctionsEntity.setProperty("player", playerName);
			auctionsEntity.setProperty("itemTsid", class_tsid);
			auctionsEntity.setUnindexedProperty("cost", cost);
			auctionsEntity.setUnindexedProperty("count", count);
						
			memcache.put(auctionID, auctionsEntity);
			memcache.put(class_tsid, historyEntity);
			replaceOrAdd(historyEntity);
			auctionEntities.add(auctionsEntity);
			currentAuctions.add(auctionsEntity);
			
			if(cost/count < weightedCost*.8)
			{
				if(memcache.contains("hot deal"))
				{
					Entity e = (Entity) memcache.get("hot deal");
					int ecount = (Integer) e.getProperty("count");
					double ecost = (Double) e.getProperty("cost");
					double eavgCost = (Double) e.getProperty("avgCost");
					if(cost/count/weightedCost < ecost/ecount/eavgCost)
					{
						auctionsEntity.setProperty("avgCost", weightedCost);
						auctionsEntity.setProperty("name", name);
						memcache.put("hot deal", auctionsEntity);
					}
				}
				else
				{
					auctionsEntity.setProperty("avgCost", weightedCost);
					auctionsEntity.setProperty("name", name);
					memcache.put("hot deal", auctionsEntity);
				}
			}
		}
	}
	
	private void replaceOrAdd(Entity entity)
	{
		for(int i=0; i<averageEntities.size(); i++)
		{
			if(averageEntities.get(i).getKey().getName().equals(entity.getKey().getName()))
			{
				averageEntities.set(i, entity);
				return;
			}				
		}
		
		averageEntities.add(entity);
	}
	
	private void purgeSoldEntities()
	{
		ArrayList<Key> soldList = new ArrayList<Key>();
		
		for(Entity e : previousAuctions)
		{
			boolean found = false;
			for(Entity k : currentAuctions)
			{
				if(e.getKey().equals(k.getKey()))
				{
					found = true;
					break;
				}
			}
			
			if(!found)
				soldList.add(e.getKey());
		}
		
		for(Key k : soldList)
		{
			memcache.delete(k.getName());
		}
		
		datastore.delete(soldList);
		
		System.out.println("Sold entries deleted: " + soldList.size());
		previousAuctions = currentAuctions;
	}
	
	private void purgeExpiredEntities()
	{
		Query q = new Query("Auctions");
		long timeLong = Calendar.getInstance().getTimeInMillis()/1000-86400;
		String keyString = timeLong + "-AAAAAAAAAAAAAAA";
		q.addFilter("__key__", Query.FilterOperator.LESS_THAN, KeyFactory.createKey("Auctions", keyString));
		PreparedQuery pq = datastore.prepare(q);
		ArrayList<Key> results = new ArrayList<Key>();
		
		for(Entity result : pq.asIterable())
		{
			Key k = result.getKey();
			results.add(k);
			memcache.delete(k.getName());
			
			//these auctions probably expired (could have been sold in between scans)
			//so we shouldn't count them in the averages
			long count = (Long) result.getProperty("count");
			double cost = (Double) result.getProperty("cost");
			String tsid = (String) result.getProperty("itemTsid");
			
			numAuctionsDeleted++;
			
			Query averageQ = new Query("Averages");
			averageQ.addFilter("__key__", Query.FilterOperator.EQUAL, KeyFactory.createKey("Averages",tsid));
			PreparedQuery averagePQ = datastore.prepare(averageQ);
			for(Entity averageResult : averagePQ.asIterable())
			{
				double costHistory = (Double) averageResult.getProperty("cost");
				long seen = (Long) averageResult.getProperty("seen");				
				double weightedCost = (costHistory*seen - cost)/(seen-count);
				Entity newAverage = new Entity("Averages", tsid);
				newAverage.setProperty("seen", seen-count);
				newAverage.setProperty("cost", weightedCost);
				datastore.put(newAverage);
				memcache.put(tsid, newAverage);
			}
		}
		
		datastore.delete(results);
	}
}
