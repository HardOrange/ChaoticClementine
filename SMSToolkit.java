package com.hardorange.clementine;

import java.util.logging.Logger;
import java.io.IOException;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;


public class SMSToolkit {
	
	private static final Logger log = Logger.getLogger(SMSToolkit.class.getName());
	
	public static String prepString(String string) {
		String sansSpace = string.replaceAll("\\s","%20");
		log.fine(sansSpace);
		return sansSpace;
	}

	public static void addMsgToQueue(String text, String msisdn) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(TaskOptions.Builder.withUrl("/api/send/sms").param("text", text).param("msisdn", msisdn));
	}
	
	public static void parseSMS(String text, String msisdn) throws IOException {
		log.fine("PARSE SMS: " + text + "FROM:  " + msisdn);
		User thisUser = UserToolkit.fetchUser(msisdn);
		
		
		if (thisUser == null) {
			if (text.toLowerCase().startsWith("signup")) {
				String[] words = text.split("\\s");
				String name = "";
				if (words.length >= 2) {
					for(int i=1;i<=words.length-1;i++){
						if(i == 1){
							name = name + words[i];
						} else {
							name = name + " " + words[i];
							
						}
					}
					UserToolkit.createUser(msisdn, name);
					addMsgToQueue(prepString("Welcome to Chaotic Clementine, " + name), msisdn);
					addMsgToQueue(prepString(Global.uHelp), msisdn);
					return;
				} else {
					addMsgToQueue(prepString(Global.helpSignup), msisdn);
					return;
				}
			} else {
				addMsgToQueue(prepString(Global.help), msisdn);
				return;
			}
		}
		
		if (text.toLowerCase().equals("help")) {
			addMsgToQueue(prepString(Global.uHelp), msisdn);
			return;
		}
		
		if (text.toLowerCase().equals("stop")) {
			UserToolkit.deleteUser(msisdn);
			addMsgToQueue(prepString("You will receive no more messages from this service."), msisdn);
			return;
		}
		
		if (text.toLowerCase().startsWith("create")) {
			String[] params = text.split("\\s");
			String name = "";
			
			for(int i = 1; i < params.length-2; i++) {
				if (i == 1) {
					name = name + params[i];
				} else {
					name = name + " " + params[i];
				}
			}
			name = name.replaceAll("\"","");
			log.fine(name);
			if (params.length >= 4) {
				EventToolkit.createEvent(name, params[params.length-2], params[params.length-1], thisUser.name);
			}
			return;
		}
		
		if (text.toLowerCase().equals("list events")) {
			EventToolkit.listEvents(msisdn);
			return;
		}
		
		if (text.toLowerCase().equals("list users")) {
			UserToolkit.listUsers(msisdn);
			return;
		}
		
		if (text.toLowerCase().startsWith("signup")) {
			addMsgToQueue(prepString("You have already signed up, text 'stop' to stop service"), msisdn);
			return;
		}
		
		if (text.toLowerCase().startsWith("no")) {
			String[] params = text.split("\\s");
			if (params.length == 2) {
				EventToolkit.eventVote(thisUser, Long.parseLong(params[1]), false);
			} else {
				addMsgToQueue("Unable to parse message, send HELP for info.", msisdn);
			}
			return;
		}
		
		if (text.toLowerCase().startsWith("yes")) {
			String[] params = text.split("\\s");
			if (params.length == 2) {
				EventToolkit.eventVote(thisUser, Long.parseLong(params[1]), true);
			} else {
				addMsgToQueue("Unable to parse message, send HELP for info.", msisdn);
			}
			return;
		}
		
		if (text.toLowerCase().startsWith("snooze")) {
			String[] params = text.split("\\s");
			if (params.length == 2) {
				addMsgToQueue(prepString("You have been snoozed for " + params[1] + " minutes."), msisdn);
				UserToolkit.snoozeUser(msisdn, params[1]);
				return;
			} else {
				addMsgToQueue("Unable to parse message, send HELP for info.", msisdn);
				return;
			}
		}
		
		if (text.toLowerCase().equals("unsnooze")) {
			UserToolkit.unSnoozeUser(msisdn);
			addMsgToQueue(prepString("You have been unsnoozed."), msisdn);
			return;
		}
		
		if (text.toLowerCase().startsWith("status")) {
			String[] params = text.split("\\s");
			if (params.length == 2) {
				EventToolkit.eventStatus(Long.parseLong(params[1]), msisdn);
			} else {
				addMsgToQueue("Unable to parse message, send HELP for info.", msisdn);
			}
			return;
		}
		
		if (text.toLowerCase().startsWith("cancel")) {
			String[] params = text.split("\\s");
			if (params.length == 2) {
				EventToolkit.cancelEvent(Long.parseLong(params[1]), thisUser.name, msisdn);
			} else {
				addMsgToQueue("Unable to parse message, send HELP for info.", msisdn);
			}
			return;
		}
		
		addMsgToQueue(prepString(Global.uHelp), msisdn);
	}
}