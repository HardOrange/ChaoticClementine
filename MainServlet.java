package com.hardorange.clementine;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.http.*;

@SuppressWarnings("serial")
public class MainServlet extends HttpServlet {
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		resp.setContentType("text/html");
		PrintWriter out = resp.getWriter();
		
		out.println("<html><body>");
		out.println("I am Chaotic Clementine!");
		out.println("I am a service accessible via text message!");
		
		out.println("Please text me at: 1-202-888-6162");
		
		out.println("Copyright 2013 Hard Orange LLC.");
		
		out.println("</body></html>");
		
		
		
	}
}
