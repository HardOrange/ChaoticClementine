package com.hardorange.clementine;

import java.util.Date;
import java.util.ArrayList;

import com.google.appengine.api.datastore.*;

public class EventToolkit {

	private static DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public static void createEvent(String eName, String minPeople, String timeout, String creator) {
		
		Entity e = new Entity("event");
		
		e.setProperty("name", eName);
		e.setProperty("minPeople" , Integer.parseInt(minPeople));
		Date d = new Date();
		e.setProperty("timeout", new Date(d.getTime() + Integer.parseInt(timeout) * 60000));
		e.setProperty("creator", creator);
		ArrayList<String> yesusr = new ArrayList<String>();
		yesusr.add(creator);
		e.setProperty("yes", yesusr );
		e.setProperty("no", new ArrayList<String>() );
		
		datastore.put(e);
		
		notifyNewEvent(eName, creator, minPeople, timeout, e.getKey().getId());
		
	}
	@SuppressWarnings("deprecation")
	public static void listEvents(String msisdn) {
		String message = "Current Events Are:%0a";
		Query q = new Query("event");
		PreparedQuery pq = datastore.prepare(q);
		if(pq.countEntities() == 0){
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("There are no active events."), msisdn);
			return;
		}
		for (Entity e : pq.asIterable()) {
			message = message + "-" + e.getProperty("name") + "%0a";
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
		
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e : pq.asIterable()) {
			String usrname = (String) e.getProperty("name");
			if ( usrname.equals(creator)) {
				SMSToolkit.addMsgToQueue(SMSToolkit.prepString( "You have created event: '" + eventName + "' which requires at least " + minPeople + " attendees and will expire in " + mins + 
						" minutes. Send CANCEL and the event ID: " + id + " to cancel event"), e.getKey().getName());
				continue;
			}
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString(message), e.getKey().getName());
		}
		
	}
	
	public static void NotifyCancelledEvent(String name) {
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e : pq.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED due to insufficient participants. "), e.getKey().getName());
		}
	}
	
	public static void NotifyUserCancelledEvent(String name, String creator) {
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e : pq.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED by " + creator + "."), e.getKey().getName());
		}
	}
	
	public static void NotifyDeclinedEvent(String name) {
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e : pq.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + name + "' has been CANCELLED. Too many users unable to attend. "), e.getKey().getName());
		}
	}
	
	public static void cleanExpiredEvents() {
		Date d = new Date();
		
		Query q = new Query("event");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e: pq.asIterable()) {
			if (d.after( (Date) e.getProperty("timeout"))) {
				datastore.delete(e.getKey());
				NotifyCancelledEvent((String) e.getProperty("name") );
			}
		}
	}
	
	@SuppressWarnings("unchecked")
	public static void eventVote(User u, long id, boolean b) {
		Entity e = null;
		try {
			e = datastore.get(KeyFactory.createKey("event", id));
		} catch (EntityNotFoundException ex) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event not found. Send HELP for info."), u.number);
			return;
		}
		if (b) {
			ArrayList<String> yesUsers = (ArrayList<String>) e.getProperty("yes");
			if (yesUsers == null) {
				yesUsers = new ArrayList<String>();
			}
			yesUsers.add(u.name);
			e.setProperty("yes", yesUsers);
		} else {
			ArrayList<String> noUsers = (ArrayList<String>) e.getProperty("no");
			if (noUsers == null) {
				noUsers = new ArrayList<String>();
			}
			noUsers.add(u.name);
			e.setProperty("no", noUsers);
		}
		
		datastore.put(e);
		SMSToolkit.addMsgToQueue("You have responded to event: " + e.getProperty("name"), u.number);
		checkEventCounts();
	}

	@SuppressWarnings("unchecked")
	public static void checkEventCounts() {
		
		Query q2 = new Query("user");
		q2.setKeysOnly();
		PreparedQuery pq2 = datastore.prepare(q2);
		@SuppressWarnings("deprecation")
		int numUser = pq2.countEntities();
		
		Query q = new Query("event");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity e: pq.asIterable()) {
			ArrayList<String> yesList = (ArrayList<String>) e.getProperty("yes");
			ArrayList<String> noList = (ArrayList<String>) e.getProperty("no");
			
			if (yesList != null) {
				if (yesList.size() >= (long) e.getProperty("minPeople")) {
					notifyEventConfirmed(e);
					datastore.delete(e.getKey());
				}
			}
			
			
			if (noList == null) return;
			if (numUser - noList.size() < (long) e.getProperty("minPeople")) {
				NotifyDeclinedEvent((String) e.getProperty("name"));
				datastore.delete(e.getKey());
			}
			
			
		}
		
	}

	@SuppressWarnings("unchecked")
	public static void notifyEventConfirmed(Entity e) {
		ArrayList<String> yesVotes = (ArrayList<String>) e.getProperty("yes");
		
		String userList = "";
		for (String u : yesVotes) {
			userList += u;
			if (u == yesVotes.get(yesVotes.size()-1   )   ) {
				userList += ".";
			} else {
				userList += ", ";
			}
		}
		
		Query q = new Query("user");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity usr : pq.asIterable()) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Event: '" + e.getProperty("name") + "' has been CONFIRMED. People attending: " + userList), usr.getKey().getName());
		}
	}

	public static void cancelEvent(long id, String creator, String msisdn) {
		Entity e;
		try {
			e = datastore.get(KeyFactory.createKey("event", id));
		} catch (EntityNotFoundException e1) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Could not cancel event, it does not exist."), msisdn);
			return;
		}
		
		String ec = (String) e.getProperty("creator"); 
		
		if (!ec.equals(creator)) {
			SMSToolkit.addMsgToQueue(SMSToolkit.prepString("Only the event creator can cancel an event."), msisdn);
		} else {
			datastore.delete(e.getKey());
			NotifyUserCancelledEvent((String) e.getProperty("name"), creator);
		}	
	}
}
