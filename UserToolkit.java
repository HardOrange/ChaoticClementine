package com.hardorange.clementine;

import java.util.Date;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.*;

public class UserToolkit {
	
	private static final Logger log = Logger.getLogger(UserToolkit.class.getName());

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	
	public static User fetchUser(String number) {
		Key k = KeyFactory.createKey("user", number);
		Entity fetched;
		try {
			fetched = datastore.get(k);
			log.fine("Found user:  " + number);
		} catch (EntityNotFoundException e) {
			log.fine("Didn't find user:  " + number);
			return null;
		}
		User u = new User((String)fetched.getProperty("name"), fetched.getKey().getName());
		return u;
	}
	@SuppressWarnings("deprecation")
	public static void listUsers(String msisdn) {
		String message = "Current Users Are:%0a";
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		if(pq.countEntities() == 0){
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("There are no active users."), msisdn);
			return;
		}
		for (Entity e : pq.asIterable()) {
			message = message + "-" + e.getProperty("name") + "%0a";
		}
		
		SMSToolkit.addMsgToQueue(SMSToolkit.prepString(message), msisdn);
	}
	public static void createUser(String number, String name) {
		Entity e = new Entity("user", number);
		e.setProperty("name", name);
		datastore.put(e);
	}
	public static void snoozeUser(String number, String snoozeTime){
		Entity e = new Entity("snoozers", number);
		Date d = new Date();
		e.setProperty("snoozeTime", new Date(d.getTime() + Integer.parseInt(snoozeTime)*60000));
		datastore.put(e);
	}
	public static void unSnoozeUser(String number){
		datastore.delete(KeyFactory.createKey("snoozers", number));
	}
	public static void checkSnoozedUsers(){
		Date d = new Date();
		
		Query q = new Query("snoozers");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e: pq.asIterable()) {
			if (d.after( (Date) e.getProperty("snoozeTime"))) {
				datastore.delete(e.getKey());
				SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Your snooze period has expired."), e.getKey().getName());
			}
		}
	}
	public static void deleteUser(String number) {
		try {
			datastore.delete(KeyFactory.createKey("user", number));
		} catch (IllegalArgumentException e) {
			return;
		}
	}
}
