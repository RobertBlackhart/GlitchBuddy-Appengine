package com.mcdermotsoft.glitchenjournal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.ByteBuffer;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.files.AppEngineFile;
import com.google.appengine.api.files.FileService;
import com.google.appengine.api.files.FileServiceFactory;
import com.google.appengine.api.files.FileWriteChannel;

@SuppressWarnings("serial")
public class GetImageTask extends HttpServlet
{
	FileService fileService = FileServiceFactory.getFileService();
	
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{
		String iconUrl = req.getParameter("url");
		String filename = req.getParameter("tsid");
		
		try 
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			InputStream is = null;
			try 
			{
				is = new URL(iconUrl).openStream();
				byte[] byteChunk = new byte[4096]; // Or whatever size you want to read in at a time.
				int n;

				while ((n = is.read(byteChunk)) > 0 ) 
				{
					baos.write(byteChunk, 0, n);
				}
			}
			catch (IOException ex)
			{
				ex.printStackTrace ();
			}
			finally 
			{
				if (is != null) 
					is.close(); 
			}
			
			AppEngineFile file = fileService.createNewBlobFile("image/png",filename+".png");
			boolean lock = true;
			FileWriteChannel writeChannel = fileService.openWriteChannel(file, lock);
			writeChannel.write(ByteBuffer.wrap(baos.toByteArray()));
			writeChannel.closeFinally();
		} 
		catch (Exception ex) 
		{
			System.err.println(ex.getMessage());
		}
		
		resp.setContentType("text/plain");
		resp.getWriter().println("done");
	}
}
