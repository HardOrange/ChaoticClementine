package com.hardorange.clementine;

import java.io.IOException;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


@SuppressWarnings("serial")
public class ReceiveSMS extends HttpServlet{
	private static final Logger log = Logger.getLogger(ReceiveSMS.class.getName());
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		
		
		String text = req.getParameter("text");
		String msisdn = req.getParameter("msisdn");
		String id = req.getParameter("messageId");
		
		if (text == null || msisdn == null || id == null) {
			log.fine("Error receiving message");
			return;
		}
		
		log.fine("RC SMS: " + text + "  FROM: " + msisdn);
		
		SMSToolkit.parseSMS(text, msisdn);
		
		resp.setStatus(HttpServletResponse.SC_OK);
		
		return;
	}
	
}
