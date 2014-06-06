package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

@SuppressWarnings("serial")
public class GetIcon extends HttpServlet 
{
	DatastoreService datastore = DatastoreServiceFactory.getDatastoreService(); 
	private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		String filename = req.getParameter("filename");
		
		Query query = new Query("__BlobInfo__"); 
		query.addFilter("filename", FilterOperator.EQUAL, filename); 

		PreparedQuery pq = datastore.prepare(query); 
		List<Entity> results = pq.asList(FetchOptions.Builder.withLimit(1));
		
		resp.setContentType("image/png");
		BlobKey blobKey = new BlobKey(results.get(0).getKey().getName());
        blobstoreService.serve(blobKey, resp);
	}
}
