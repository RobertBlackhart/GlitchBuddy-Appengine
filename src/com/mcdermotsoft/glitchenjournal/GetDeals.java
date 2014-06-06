package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class GetDeals extends HttpServlet 
{
	MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		Entity dealEntity = (Entity) memcache.get("hot deal");
		
		String itemName = (String) dealEntity.getProperty("name");
		String playerName = (String) dealEntity.getProperty("player");
		String tsid = (String) dealEntity.getProperty("itemTsid");
		String id = dealEntity.getKey().getName();
		long count;
		double cost;
		double avgCost;
		try
		{
			long lcount = (Long) dealEntity.getProperty("count");
			count = lcount;
		}
		catch(ClassCastException ex)
		{
			int icount = (Integer) dealEntity.getProperty("count");
			count = icount;
		}
		try
		{
			double dcost = (Double) dealEntity.getProperty("cost");
			cost = dcost;
		}
		catch(ClassCastException ex)
		{
			float fcost = (Float) dealEntity.getProperty("cost");
			cost = fcost;
		}
		try
		{
			double davgCost = (Double) dealEntity.getProperty("avgCost");
			avgCost = davgCost;
		}
		catch(ClassCastException ex)
		{
			float favgCost = (Float) dealEntity.getProperty("avgCost");
			avgCost = favgCost;
		}
		
		Item item = new Item(itemName, playerName, tsid, id, count, cost, avgCost);
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(resp.getWriter(), item);
	}
}
