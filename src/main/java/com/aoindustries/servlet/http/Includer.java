/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2015, 2016, 2017  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-servlet-util.
 *
 * ao-servlet-util is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-servlet-util is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-servlet-util.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.servlet.http;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * Performs an include while allowing SkipPageException and sendError to escape
 * the include.  This is required to support redirecting from within an included
 * page.
 *
 * @author  AO Industries, Inc.
 */
public class Includer {

	private static final Logger logger = Logger.getLogger(Includer.class.getName());

	private Includer() {
	}

	/**
	 * Since sendError does not work within included pages, the outermost include
	 * sets the isIncluded flag to true.
	 */
	private static final String IS_INCLUDED_REQUEST_ATTRIBUTE_NAME = Includer.class.getName() + ".isIncluded";

	/**
	 * The location header that should be set before calling sendError.
	 */
	private static final String LOCATION_REQUEST_ATTRIBUTE_NAME = Includer.class.getName() + ".location";

	/**
	 * The status that should be sent to sendError.
	 */
	private static final String STATUS_REQUEST_ATTRIBUTE_NAME = Includer.class.getName() + ".sendError.status";

	/**
	 * The message that should be sent to sendError.
	 */
	private static final String MESSAGE_REQUEST_ATTRIBUTE_NAME = Includer.class.getName() + ".sendError.message";

	/**
	 * The request attribute name set to boolean true when the page should be skipped.
	 * This is used to propagate a SkipPageException through the include chain.
	 * This results in different behavior than a standard jsp:include but should
	 * lead to more intuitive results.  With this change a redirect may be
	 * performed within an include.
	 */
	private static final String PAGE_SKIPPED_REQUEST_ATTRIBUTE_NAME = Includer.class.getName() + ".pageSkipped";

	/**
	 * Performs the actual include, supporting propagation of SkipPageException and sendError.
	 */
	public static void dispatchInclude(RequestDispatcher dispatcher, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SkipPageException {
		final boolean isOutmostInclude = request.getAttribute(IS_INCLUDED_REQUEST_ATTRIBUTE_NAME) == null;
		if(logger.isLoggable(Level.FINE)) logger.log(
			Level.FINE, "request={0}, isOutmostInclude={1}",
			new Object[] {
				request,
				isOutmostInclude
			}
		);
		try {
			if(isOutmostInclude) request.setAttribute(IS_INCLUDED_REQUEST_ATTRIBUTE_NAME, true);
			dispatcher.include(request, response);
			if(isOutmostInclude) {
				// Set location header if set in attribute
				String location = (String)request.getAttribute(LOCATION_REQUEST_ATTRIBUTE_NAME);
				if(location != null) response.setHeader("Location", location);

				// Call sendError from here if set in attributes
				Integer status = (Integer)request.getAttribute(STATUS_REQUEST_ATTRIBUTE_NAME);
				if(status != null) {
					String message = (String)request.getAttribute(MESSAGE_REQUEST_ATTRIBUTE_NAME);
					if(message == null) {
						response.sendError(status);
					} else {
						response.sendError(status, message);
					}
				}
			}
			// Propagate effects of SkipPageException
			if(request.getAttribute(PAGE_SKIPPED_REQUEST_ATTRIBUTE_NAME) != null) throw ServletUtil.SKIP_PAGE_EXCEPTION;
		} finally {
			if(isOutmostInclude) {
				request.removeAttribute(IS_INCLUDED_REQUEST_ATTRIBUTE_NAME);
				request.removeAttribute(LOCATION_REQUEST_ATTRIBUTE_NAME);
				request.removeAttribute(STATUS_REQUEST_ATTRIBUTE_NAME);
				request.removeAttribute(MESSAGE_REQUEST_ATTRIBUTE_NAME);
				// PAGE_SKIPPED_REQUEST_ATTRIBUTE_NAME not removed to propagate fully up the stack
			}
		}
	}

	/**
	 * Sets a Location header.  When not in an included page, calls setHeader directly.
	 * When inside of an include will set request attribute so outermost include can call setHeader.
	 */
	public static void setLocation(HttpServletRequest request, HttpServletResponse response, String location) {
		if(request.getAttribute(IS_INCLUDED_REQUEST_ATTRIBUTE_NAME) == null) {
			// Not included, setHeader directly
			response.setHeader("Location", location);
		} else {
			// Is included, set attribute so top level tag can perform actual setHeader call
			request.setAttribute(LOCATION_REQUEST_ATTRIBUTE_NAME, location);
		}
	}

	/**
	 * Sends an error.  When not in an included page, calls sendError directly.
	 * When inside of an include will set request attribute so outermost include can call sendError.
	 */
	public static void sendError(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
		if(request.getAttribute(IS_INCLUDED_REQUEST_ATTRIBUTE_NAME) == null) {
			// Not included, sendError directly
			if(message == null) {
				response.sendError(status);
			} else {
				response.sendError(status, message);
			}
		} else {
			// Is included, set attributes so top level tag can perform actual sendError call
			request.setAttribute(STATUS_REQUEST_ATTRIBUTE_NAME, status);
			request.setAttribute(MESSAGE_REQUEST_ATTRIBUTE_NAME, message);
		}
	}

	public static void sendError(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
		sendError(request, response, status, null);
	}

	/**
	 * Sets the skip page flag.
	 */
	public static void setPageSkipped(ServletRequest request) {
		request.setAttribute(PAGE_SKIPPED_REQUEST_ATTRIBUTE_NAME, true);
	}
}
