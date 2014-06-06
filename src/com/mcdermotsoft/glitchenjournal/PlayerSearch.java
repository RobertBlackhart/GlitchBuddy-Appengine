package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONException;
import org.json.JSONObject;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class PlayerSearch extends HttpServlet 
{
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String playerName = req.getParameter("playerName");
		if(playerName == null || playerName.equals(""))
			return;
		String match = req.getParameter("exactMatch");
		boolean exactMatch = false;
		if(match != null && match.equals("true"))
			exactMatch = true;
		
		Query q = new Query("Auctions").setKeysOnly();
		if(exactMatch)
			q.addFilter("player", Query.FilterOperator.EQUAL, playerName);
		else
		{
			q.addFilter("player", Query.FilterOperator.GREATER_THAN_OR_EQUAL, playerName);
			q.addFilter("player", Query.FilterOperator.LESS_THAN, playerName + "\uFFFD");
		}
		
		PreparedQuery pq = datastore.prepare(q);
		ArrayList<Player> results = new ArrayList<Player>();
		for(Entity result : pq.asIterable())
		{
			Key k = result.getKey();
			if(memcache.contains(k.getName()))
			{
				Entity temp = (Entity) memcache.get(k.getName());
				String name = (String)temp.getProperty("player");
				String keyString = result.getKey().getName();
				String tsid = keyString.substring(keyString.indexOf("-")+1, keyString.length());
				Player player = new Player(name, tsid);
				if(!results.contains(player))
					results.add(player);
			}
			else
			{
				Entity temp;
				try 
				{
					temp = datastore.get(k);
					String name = (String)temp.getProperty("player");
					String keyString = result.getKey().getName();
					String tsid = keyString.substring(keyString.indexOf("-")+1, keyString.length());
					Player player = new Player(name, tsid);
					if(!results.contains(player))
						results.add(player);
				} 
				catch (EntityNotFoundException e) 
				{
					
				}
			}
		}
		
		doOutput(results, resp);
	}
	
	private void doOutput(ArrayList<Player> results, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("application/json");
		try 
		{
			resp.getWriter().println(JSONObject.valueToString(results));
		} 
		catch(JSONException ex) 
		{
			ex.printStackTrace();
		}
	}
}
