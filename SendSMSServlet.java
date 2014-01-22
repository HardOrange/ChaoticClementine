package com.hardorange.clementine;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.KeyFactory;

@SuppressWarnings("serial")
public class SendSMSServlet extends HttpServlet {

	private static final Logger log = Logger.getLogger(SendSMSServlet.class.getName());

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public void doPost(HttpServletRequest req, HttpServletResponse resp) {
		
		String text = req.getParameter("text");
		String msisdn = req.getParameter("msisdn");
		
		sendSMS(text, msisdn);
	}
	
	public static void sendSMS(String text, String number) {
		Entity e = null;
		try {
			e = datastore.get(KeyFactory.createKey("snoozers", number));
		} catch (EntityNotFoundException e1) {
			e1.printStackTrace();
		}
		if(e == null || text.toLowerCase().contains("snooze")){
			String cleansed = text.replace("&", "%26");
			cleansed = cleansed.replace("#", "%23");
		
			log.fine("SENDING SMS: " + text + " TO:  " + number);
			String s = "https://rest.nexmo.com/sms/json?api_key=API_KEY&api_secret=SECRET&from=FROM_NUMBER&to=" + number + "&text=" + cleansed;
			log.fine(s);
			URL url = null;
			try {
				url = new URL(s);
			} catch (MalformedURLException ex2) {
				ex2.printStackTrace();
			}
			try {
				url.openStream();
			} catch (IOException ex) {
				ex.printStackTrace();
			} 
			log.fine("sent");
		} else {
			log.fine("User with number: " + number + " is snoozed.");
		}
	}
	
}
