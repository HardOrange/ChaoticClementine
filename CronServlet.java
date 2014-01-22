package com.hardorange.clementine;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@SuppressWarnings("serial")
public class CronServlet extends HttpServlet {

	public void doGet(HttpServletRequest req, HttpServletResponse resp) {
		EventToolkit.cleanExpiredEvents();
		UserToolkit.checkSnoozedUsers();
	}
	
}
