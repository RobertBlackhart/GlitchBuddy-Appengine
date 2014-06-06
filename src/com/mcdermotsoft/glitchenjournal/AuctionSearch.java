package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class AuctionSearch extends HttpServlet 
{
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	ArrayList<Entity> results;
	MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	int numMem;
	Logger log = Logger.getLogger(AuctionSearch.class.getName());
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		numMem = 0;
		
		String searchText = req.getParameter("searchText");
		if(searchText == null || searchText.equals(""))
			return;
		String player = req.getParameter("isPlayer");
		boolean isPlayer = false;
		if(player != null && player.equals("true"))
			isPlayer = true;
		String match = req.getParameter("exactMatch");
		boolean exactMatch = false;
		if(match != null && match.equals("true"))
			exactMatch = true;
				
		if(isPlayer)
			results = getPlayerAuctions(searchText);
		else
			results = getFromCacheOrStore(searchText, exactMatch);
		
		ArrayList<Item> items = new ArrayList<Item>();
		for(Entity entity : results)
		{
			String itemName = (String) entity.getProperty("name");
			String playerName = (String) entity.getProperty("player");
			String tsid = (String) entity.getProperty("itemTsid");
			String id = entity.getKey().getName();
			long count;
			double cost;
			double avgCost;
			try
			{
				long lcount = (Long) entity.getProperty("count");
				count = lcount;
			}
			catch(ClassCastException ex)
			{
				int icount = (Integer) entity.getProperty("count");
				count = icount;
			}
			try
			{
				double dcost = (Double) entity.getProperty("cost");
				cost = dcost;
			}
			catch(ClassCastException ex)
			{
				float fcost = (Float) entity.getProperty("cost");
				cost = fcost;
			}
			try
			{
				double davgCost = (Double) entity.getProperty("avgCost");
				avgCost = davgCost;
			}
			catch(ClassCastException ex)
			{
				float favgCost = (Float) entity.getProperty("avgCost");
				avgCost = favgCost;
			}
			items.add(new Item(itemName, playerName, tsid, id, count, cost, avgCost));
		}
		
		doOutput(items, resp);
	}
	
	private ArrayList<Entity> getPlayerAuctions(String playerName)
	{
		ArrayList<Entity> results = new ArrayList<Entity>();
		
		Query q = new Query("Auctions");
		q.addFilter("player", Query.FilterOperator.EQUAL, playerName);
		PreparedQuery pq = datastore.prepare(q);
				
		for(Entity e : pq.asIterable())
		{
			if(memcache.contains(e.getProperty("itemTsid")))
			{
				Entity temp = (Entity)memcache.get(e.getProperty("itemTsid"));
				e.setProperty("avgCost", temp.getProperty("cost"));
				e.setProperty("name", temp.getProperty("name"));
				results.add(e);
				numMem++;
			}
			else
			{
				Query avgq = new Query("Averages");
				avgq.addFilter("__key__", Query.FilterOperator.EQUAL, KeyFactory.createKey("Averages", (String)e.getProperty("itemTsid")));
				PreparedQuery avgpq = datastore.prepare(avgq);
				
				for(Entity avge : avgpq.asIterable())
				{
					Entity result = e;
					result.setProperty("avgCost", avge.getProperty("cost"));
					result.setProperty("name", avge.getProperty("name"));
					results.add(result);
				}
			}
		}
		
		return results;
	}
	
	private void doOutput(ArrayList<Item> results, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("application/json");
		log.info(("Num Results: " + results.size() + ", Num from Memcache: " + numMem));
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(resp.getWriter(), results);
	}
	
	private ArrayList<Entity> getFromCacheOrStore(String searchText, boolean exactMatch)
	{
		ArrayList<Entity> results = new ArrayList<Entity>();
		ArrayList<Entity> datastoreGet = new ArrayList<Entity>();
		
		Query q = new Query("Averages");
		if(exactMatch)
			q.addFilter("name", Query.FilterOperator.EQUAL, searchText);
		else
		{
			q.addFilter("name", Query.FilterOperator.GREATER_THAN_OR_EQUAL, searchText);
			q.addFilter("name", Query.FilterOperator.LESS_THAN, searchText + "\uFFFD");
		}
		PreparedQuery pq = datastore.prepare(q);
		
		for(Entity result : pq.asIterable())
		{
			Query aucQ = new Query("Auctions").setKeysOnly();
			aucQ.addFilter("itemTsid", Query.FilterOperator.EQUAL, result.getKey().getName());
			PreparedQuery aucPQ = datastore.prepare(aucQ);
			
			for(Entity e : aucPQ.asIterable())
			{
				if(memcache.contains(e.getKey().getName()))
				{
					Entity res = (Entity)memcache.get(e.getKey().getName());
					res.setProperty("name", result.getProperty("name"));
					res.setProperty("avgCost", result.getProperty("cost"));
					results.add(res);
					numMem++;
				}
				else
				{
					e.setProperty("name", result.getProperty("name"));
					e.setProperty("avgCost", result.getProperty("cost"));
					datastoreGet.add(e);
				}
			}
		}
		
		for(Entity e : datastoreGet)
		{
			try
			{
				Entity temp = datastore.get(e.getKey());
				temp.setProperty("name", e.getProperty("name"));
				temp.setProperty("avgCost", e.getProperty("avgCost"));
				results.add(temp);
				memcache.put(temp.getKey().getName(), temp);
			}
			catch(EntityNotFoundException ex)
			{
				
			}
		}
		
		return results;
	}
}
