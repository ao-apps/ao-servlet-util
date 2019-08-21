/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2016, 2017  AO Industries, Inc.
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
package com.aoindustries.servlet;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * Automatically adds and removes the cache.
 *
 * @see  ServletContextCache
 *
 * @author  AO Industries, Inc.
 */
@WebListener
final public class ServletContextCacheListener implements ServletContextListener {

	private ServletContextCache cache;

	@Override
	public void contextInitialized(ServletContextEvent event) {
		ServletContext servletContext = event.getServletContext();
		// Might have been already created
		cache = (ServletContextCache)servletContext.getAttribute(ServletContextCache.ATTRIBUTE_KEY);
		if(cache == null) {
			cache = new ServletContextCache(servletContext);
			servletContext.setAttribute(ServletContextCache.ATTRIBUTE_KEY, cache);
		} else {
			assert cache.servletContext == servletContext;
		}
	}

	@Override
	public void contextDestroyed(ServletContextEvent event) {
		ServletContext servletContext = event.getServletContext();
		servletContext.removeAttribute(ServletContextCache.ATTRIBUTE_KEY);
		if(cache != null) {
			cache.stop();
			cache = null;
		}
	}
}
