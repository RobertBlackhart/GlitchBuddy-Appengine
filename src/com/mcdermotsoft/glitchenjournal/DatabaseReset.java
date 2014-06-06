package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;
import java.util.ArrayList;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;

@SuppressWarnings("serial")
public class DatabaseReset extends HttpServlet 
{
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	BlobstoreService blobstore = BlobstoreServiceFactory.getBlobstoreService();
	MemcacheService memcache = MemcacheServiceFactory.getMemcacheService();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		deleteEntityKind("Auctions");
		deleteEntityKind("Averages");
		deleteEntityKind("__BlobInfo__");
		memcache.clearAll();
	}
	
	private void deleteEntityKind(String kind)
	{
		Query q = new Query(kind).setKeysOnly();

		PreparedQuery pq = datastore.prepare(q);

		ArrayList<Key> keys = new ArrayList<Key>();
		for(Entity e : pq.asIterable()) 
		{
			keys.add(e.getKey());
			if(kind.equals("Averages"))
				blobstore.delete(new BlobKey(e.getKey().getName()));
		}
		
		datastore.delete(keys);
	}
}
