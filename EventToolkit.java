package com.hardorange.clementine;

import java.util.Date;
import java.util.ArrayList;

import com.google.appengine.api.datastore.*;

public class EventToolkit {

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public static void createEvent(String eName, String minPeople, String timeout, String creator) {
		
		Entity entity = new Entity("event");
		
		entity.setProperty("name", eName);
		entity.setProperty("minPeople" , Integer.parseInt(minPeople));
		Date date = new Date();
		entity.setProperty("timeout", new Date(date.getTime() + Integer.parseInt(timeout) * 60000));
		entity.setProperty("creator", creator);
		ArrayList<String> yesusr = new ArrayList<String>();
		yesusr.add(creator);
		entity.setProperty("yes", yesusr );
		entity.setProperty("no", new ArrayList<String>() );
		
		datastore.put(entity);
		
		notifyNewEvent(eName, creator, minPeople, timeout, entity.getKey().getId());
		
	}
	@SuppressWarnings("deprecation")
	public static void listEvents(String msisdn) {
		String message = "Current Events Are:%0a";
		Query query = new Query("event");
		PreparedQuery preppedQuery = datastore.prepare(query);
		if(preppedQuery.countEntities() == 0){
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("There are no active events."), msisdn);
			return;
		}
		for (Entity entity : preppedQuery.asIterable()) {
			message = message + "-" + entity.getProperty("name") + "%0a";
		}
		
		SMSToolkit.addMsgToQueue(SMSToolkit.prepString(message), msisdn);
	}
	public static void eventStatus(long id, String msisdn){
		Entity currentEvent = null;
		Key event = KeyFactory.createKey("event", id);
		try {
			currentEvent = datastore.get(event);
		} catch (EntityNotFoundException e) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event not found. Send HELP for info."), msisdn);
			e.printStackTrace();
			return;
		}
		SMSToolkit.addMsgToQueue(SMSToolkit.prepString(currentEvent.getProperty("creator") + "'s event, '" + currentEvent.getProperty("name") + "', has " + currentEvent.getProperty("timeout") + " minutes remaining to be confirmed."), msisdn);
	}
	public static void notifyNewEvent(String eventName, String creator, String minPeople, String mins, long id) {
		String message = creator + " has created event:%0a" + eventName + "%0awhich needs at least%0a" + minPeople + " people%0a and will expire in%0a" + mins + 
						" mins.%0a Reply 'YES " + id + "' or 'NO " + id + "'"; 
		
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity : preppedQuery.asIterable()) {
			String usrname = (String) entity.getProperty("name");
			if ( usrname.equals(creator)) {
				SMSToolkit.addMsgToQueue(SMSToolkit.prepString( "You have created event: '" + eventName + "' which requires at least " + minPeople + " attendees and will expire in " + mins + 
						" minutes. Send CANCEL and the event ID: " + id + " to cancel event"), entity.getKey().getName());
				continue;
			}
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString(message), entity.getKey().getName());
		}
		
	}
	
	public static void NotifyCancelledEvent(String name) {
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity : preppedQuery.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED due to insufficient participants. "), entity.getKey().getName());
		}
	}
	
	public static void NotifyUserCancelledEvent(String name, String creator) {
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity : preppedQuery.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED by " + creator + "."), entity.getKey().getName());
		}
	}
	
	public static void NotifyDeclinedEvent(String name) {
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity : preppedQuery.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED. Too many users unable to attend. "), entity.getKey().getName());
		}
	}
	
	public static void cleanExpiredEvents() {
		Date date = new Date();
		
		Query query = new Query("event");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity entity: preppedQuery.asIterable()) {
			if (date.after( (Date) entity.getProperty("timeout"))) {
				datastore.delete(entity.getKey());
				NotifyCancelledEvent((String) entity.getProperty("name") );
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void eventVote(User user, long id, boolean b) {
		Entity entity = null;
		try {
			entity = datastore.get(KeyFactory.createKey("event", id));
		} catch (EntityNotFoundException ex) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event not found. Send HELP for info."), user.number);
			return;
		}
		if (b) {
			ArrayList<String> yesUsers = (ArrayList<String>) e.getProperty("yes");
			if (yesUsers == null) {
				yesUsers = new ArrayList<String>();
			}
			yesUsers.add(user.name);
			entity.setProperty("yes", yesUsers);
		} else {
			ArrayList<String> noUsers = (ArrayList<String>) entity.getProperty("no");
			if (noUsers == null) {
				noUsers = new ArrayList<String>();
			}
			noUsers.add(user.name);
			entity.setProperty("no", noUsers);
		}
		
		datastore.put(entity);
		SMSToolkit.addMsgToQueue("You have responded to event: " + entity.getProperty("name"), user.number);
		checkEventCounts();
	}

	@SuppressWarnings("unchecked")
	public static void checkEventCounts() {
		
		Query userQuery = new Query("user");
		userQuery.setKeysOnly();
		PreparedQuery userPreppedQuery = datastore.prepare(userQuery);
		@SuppressWarnings("deprecation")
		int numUser = userPreppedQuery.countEntities();
		
		Query eventQuery = new Query("event");
		PreparedQuery eventPreppedQuery = datastore.prepare(eventQuery);
		for (Entity entity: eventPreppedQuery.asIterable()) {
			ArrayList<String> yesList = (ArrayList<String>) entity.getProperty("yes");
			ArrayList<String> noList = (ArrayList<String>) entity.getProperty("no");
			
			if (yesList != null) {
				if (yesList.size() >= (long) entity.getProperty("minPeople")) {
					notifyEventConfirmed(entity);
					datastore.delete(entity.getKey());
				}
			}
			
			
			if (noList == null) return;
			if (numUser - noList.size() < (long) entity.getProperty("minPeople")) {
				NotifyDeclinedEvent((String) entity.getProperty("name"));
				datastore.delete(entity.getKey());
			}
			
			
		}
		
	}

	@SuppressWarnings("unchecked")
	public static void notifyEventConfirmed(Entity entity) {
		ArrayList<String> yesVotes = (ArrayList<String>) entity.getProperty("yes");
		
		String userList = "";
		for (String user : yesVotes) {
			userList += user;
			if (user == yesVotes.get(yesVotes.size()-1   )   ) {
				userList += ".";
			} else {
				userList += ", ";
			}
		}
		
		Query query = new Query("user");
		PreparedQuery preppedQuery = datastore.prepare(query);
		for (Entity usr : preppedQuery.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + entity.getProperty("name") + "' has been CONFIRMED. People attending: " + userList), usr.getKey().getName());
		}
	}

	public static void cancelEvent(long id, String creator, String msisdn) {
		Entity entity;
		try {
			entity = datastore.get(KeyFactory.createKey("event", id));
		} catch (EntityNotFoundException e) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Could not cancel event, it does not exist."), msisdn);
			return;
		}
		
		String eventCreator = (String) entity.getProperty("creator"); 
		
		if (!eventCreator.equals(creator)) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Only the event creator can cancel an event."), msisdn);
		} else {
			datastore.delete(entity.getKey());
			NotifyUserCancelledEvent((String) entity.getProperty("name"), creator);
		}	
	}
}
