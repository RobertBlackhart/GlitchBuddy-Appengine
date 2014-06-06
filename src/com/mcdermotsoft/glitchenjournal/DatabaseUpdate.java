package com.mcdermotsoft.glitchenjournal;

import static com.google.appengine.api.taskqueue.TaskOptions.Builder.withUrl;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions.Method;

@SuppressWarnings("serial")
public class DatabaseUpdate extends HttpServlet 
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException 
	{				
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(withUrl("/cron/databasetask").method(Method.GET));
	}
}
