/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2015, 2016, 2017, 2019, 2020, 2021  AO Industries, Inc.
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
package com.aoapps.servlet.http;

import com.aoapps.net.URIEncoder;
import com.aoapps.servlet.ServletUtil;
import com.aoapps.servlet.attribute.AttributeEE;
import com.aoapps.servlet.attribute.ScopeEE;
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
	private static final ScopeEE.Request.Attribute<Boolean> IS_INCLUDED_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(Includer.class.getName() + ".isIncluded");

	/**
	 * The location header that should be set before calling sendError.
	 */
	private static final ScopeEE.Request.Attribute<String> LOCATION_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(Includer.class.getName() + ".location");

	/**
	 * The status that should be sent to sendError.
	 */
	private static final ScopeEE.Request.Attribute<Integer> STATUS_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(Includer.class.getName() + ".sendError.status");

	/**
	 * The message that should be sent to sendError.
	 */
	private static final ScopeEE.Request.Attribute<String> MESSAGE_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(Includer.class.getName() + ".sendError.message");

	/**
	 * The request attribute name set to boolean true when the page should be skipped.
	 * This is used to propagate a SkipPageException through the include chain.
	 * This results in different behavior than a standard jsp:include but should
	 * lead to more intuitive results.  With this change a redirect may be
	 * performed within an include.
	 */
	private static final ScopeEE.Request.Attribute<Boolean> PAGE_SKIPPED_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(Includer.class.getName() + ".pageSkipped");

	/**
	 * Performs the actual include, supporting propagation of SkipPageException and sendError.
	 */
	public static void dispatchInclude(RequestDispatcher dispatcher, HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException, SkipPageException {
		AttributeEE.Request<Boolean> isIncludedAttribute = IS_INCLUDED_REQUEST_ATTRIBUTE.context(request);
		final boolean isOutmostInclude = isIncludedAttribute.get() == null;
		if(logger.isLoggable(Level.FINER)) logger.log(
			Level.FINER, "request={0}, isOutmostInclude={1}",
			new Object[] {
				request,
				isOutmostInclude
			}
		);
		AttributeEE.Request<String> locationAttribute = LOCATION_REQUEST_ATTRIBUTE.context(request);
		AttributeEE.Request<Integer> statusAttribute = STATUS_REQUEST_ATTRIBUTE.context(request);
		AttributeEE.Request<String> messageAttribute = MESSAGE_REQUEST_ATTRIBUTE.context(request);
		AttributeEE.Request<Boolean> pageSkippedAttribute = PAGE_SKIPPED_REQUEST_ATTRIBUTE.context(request);
		try {
			if(isOutmostInclude) isIncludedAttribute.set(true);
			dispatcher.include(request, response);
			if(isOutmostInclude) {
				// Set location header if set in attribute
				String location = locationAttribute.get();
				if(location != null) {
					assert location.equals(URIEncoder.encodeURI(location));
					response.setHeader("Location", location);
				}

				// Call sendError from here if set in attributes
				Integer status = statusAttribute.get();
				if(status != null) {
					String message = messageAttribute.get();
					if(message == null) {
						response.sendError(status);
					} else {
						response.sendError(status, message);
					}
				}
			}
			// Propagate effects of SkipPageException
			if(pageSkippedAttribute.get() != null) throw ServletUtil.SKIP_PAGE_EXCEPTION;
		} finally {
			if(isOutmostInclude) {
				isIncludedAttribute.remove();
				locationAttribute.remove();
				statusAttribute.remove();
				messageAttribute.remove();
				// pageSkippedAttribute not removed to propagate fully up the stack
			}
		}
	}

	/**
	 * Sets a Location header.  When not in an included page, calls setHeader directly.
	 * When inside of an include will set request attribute so outermost include can call setHeader.
	 * Encodes the location to US-ASCII format.
	 */
	public static void setLocation(HttpServletRequest request, HttpServletResponse response, String location) {
		location = URIEncoder.encodeURI(location);
		if(IS_INCLUDED_REQUEST_ATTRIBUTE.context(request).get() == null) {
			// Not included, setHeader directly
			response.setHeader("Location", location);
		} else {
			// Is included, set attribute so top level tag can perform actual setHeader call
			LOCATION_REQUEST_ATTRIBUTE.context(request).set(location);
		}
	}

	/**
	 * Sends an error.  When not in an included page, calls sendError directly.
	 * When inside of an include will set request attribute so outermost include can call sendError.
	 */
	public static void sendError(HttpServletRequest request, HttpServletResponse response, int status, String message) throws IOException {
		if(IS_INCLUDED_REQUEST_ATTRIBUTE.context(request).get() == null) {
			// Not included, sendError directly
			if(message == null) {
				response.sendError(status);
			} else {
				response.sendError(status, message);
			}
		} else {
			// Is included, set attributes so top level tag can perform actual sendError call
			STATUS_REQUEST_ATTRIBUTE.context(request).set(status);
			MESSAGE_REQUEST_ATTRIBUTE.context(request).set(message);
		}
	}

	public static void sendError(HttpServletRequest request, HttpServletResponse response, int status) throws IOException {
		sendError(request, response, status, null);
	}

	/**
	 * Sets the skip page flag.
	 */
	public static void setPageSkipped(ServletRequest request) {
		PAGE_SKIPPED_REQUEST_ATTRIBUTE.context(request).set(true);
	}
}
