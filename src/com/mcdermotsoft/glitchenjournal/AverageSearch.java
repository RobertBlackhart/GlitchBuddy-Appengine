package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class AverageSearch extends HttpServlet 
{
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		String searchText = req.getParameter("searchText");
		
		Query q = new Query("Averages");
		q.addFilter("__key__", Query.FilterOperator.EQUAL, KeyFactory.createKey("Averages", searchText));
		PreparedQuery pq = datastore.prepare(q);
		
		double avgPrice = 0;
		
		for(Entity e : pq.asIterable())
		{
			avgPrice = (Double)e.getProperty("cost");
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println(avgPrice);
	}
}
