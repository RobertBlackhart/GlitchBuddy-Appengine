package com.mcdermotsoft.glitchenjournal;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class HelloWorld extends HttpServlet 
{
	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException
	{
		resp.setContentType("text/plain");
		resp.getWriter().println("Hello World");
	}
}
