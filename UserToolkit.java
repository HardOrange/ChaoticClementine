package com.hardorange.clementine;

import java.util.Date;
import java.util.logging.Logger;

import com.google.appengine.api.datastore.*;

public class UserToolkit {
	
	private static final Logger log = Logger.getLogger(UserToolkit.class.getName());

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	/*
	*Fetches a User from the Datastore. Logs Entry into log.
	*If the User is not Found, log the entry, and return null
	*@param	phoneNumber	The Phone Number of the User for Lookup
	*@return			The User that is in the datastore, otherwise null
	*
	*/
	public static User fetchUser(String phoneNumber) {
		Key key = KeyFactory.createKey("user", phoneNumber);
		Entity fetched;
		try {
			fetched = datastore.get(key);
			log.fine("Found user:  " + phoneNumber);
		} catch (EntityNotFoundException e) {
			log.fine("Didn't find user:  " + phoneNumber);
			return null;
		}
		User user = new User((String)fetched.getProperty("name"), fetched.getKey().getName());
		return user;
	}

	/*
	*@param	msisdn	Mobile Phone Number that will recieve the message.
	*
	*/
	@SuppressWarnings("deprecation")
	public static void listUsers(String msisdn) {
		String message = "Current Users Are:%0a";
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		if(preppedQuery.countEntities() == 0){
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("There are no active users."), msisdn);
			return;
		}
		for (Entity entity : preppedQuery.asIterable()) {
			message = message + "-" + entity.getProperty("name") + "%0a";
		}
		
		SMSToolkit.addMsgToQueue(SMSToolkit.prepString(message), msisdn);
	}

	/*
	*@param	phoneNumber	Mobile Telephone Number that will be registered to the Service
	*@param	name 		Username for the user (Given By User)
	*
	*
	*/
	public static void createUser(String phoneNumber, String name) {
		Entity entity = new Entity("user", phoneNumber);
		entity.setProperty("name", name);
		datastore.put(entity);
	}

	/*
	*Snoozing means the User will not recieve any SMS messages during that time
	*
	*@param phoneNumber	Phone Number of User that will be snoozed
	*@param snoozeTime	Time in minutes to be snoozed for
	*
	*/
	public static void snoozeUser(String phoneNumber, String snoozeTime){
		Entity entity = new Entity("snoozers", phoneNumber);
		Date date = new Date();
		entity.setProperty("snoozeTime", new Date(date.getTime() + Integer.parseInt(snoozeTime)*60000));
		datastore.put(entity);
	}

	/*
	*Unsnoozed the User when they need to be unsnoozed
	*@param phoneNumber	The Mobile Phone of the User that will be unsnoozed.
	*
	*/
	public static void unSnoozeUser(String phoneNumber){
		datastore.delete(KeyFactory.createKey("snoozers", phoneNumber));
	}

	/*
	*Checkes the users that are snoozed. If time has been passed, unsnooze those users.
	*
	*
	*/
	public static void checkSnoozedUsers(){
		Date date = new Date();
		
		Query query = new Query("snoozers");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity: preppedQuery.asIterable()) {
			if (date.after( (Date) entity.getProperty("snoozeTime"))) {
				//datastore.delete(entity.getKey()); Changed so that use of internal snooze Function.
				unSnoozeUser(entity.getKey().getName());
				SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Your snooze period has expired."), entity.getKey().getName());
			}
		}
	}

	/*
	*Deletes user from the System
	*@param phoneNumber	The Mobile phone number of the user to be deleted from the system.
	*

	Joe: does this run the risk of trying to delete a user that doesnt exist? Does it Catch that
	Joe: doe the illegalArgumentException Do anything?
	Joe: Why no log?
	*/
	public static void deleteUser(String phoneNumber) {
		try {
			datastore.delete(KeyFactory.createKey("user", phoneNumber));
		} catch (IllegalArgumentException e) {
			return;
		}
	}
}
