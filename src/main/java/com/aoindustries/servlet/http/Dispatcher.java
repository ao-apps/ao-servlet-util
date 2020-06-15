/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2015, 2016, 2018, 2020  AO Industries, Inc.
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

import com.aoindustries.collections.AoCollections;
import com.aoindustries.net.URIResolver;
import com.aoindustries.servlet.LocalizedServletException;
import static com.aoindustries.servlet.http.ApplicationResources.accessor;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * Static utilities that may be useful by servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 */
public class Dispatcher {

	private static final Logger logger = Logger.getLogger(Dispatcher.class.getName());

	/**
	 * The name of the request-scope Map that will contain the arguments for the current page.
	 */
	public static final String ARG_REQUEST_ATTRIBUTE = "arg";

	private Dispatcher() {
	}

	/**
	 * Tracks the first servlet path seen, before any include/forward.
	 */
	private static final String ORIGINAL_PAGE_REQUEST_ATTRIBUTE = Dispatcher.class.getName() + ".originalPage";

	/**
	 * Gets the current request original page or null if not set.
	 *
	 * @see  #getOriginalPagePath(javax.servlet.http.HttpServletRequest) for the version that uses current request as a default.
	 * @see  RequestDispatcher#FORWARD_SERVLET_PATH
	 * @see  RequestDispatcher#INCLUDE_SERVLET_PATH
	 * @see  HttpServletRequest#getServletPath()
	 */
	public static String getOriginalPage(ServletRequest request) {
		String originalPage = (String)request.getAttribute(ORIGINAL_PAGE_REQUEST_ATTRIBUTE);
		if(originalPage == null) {
			originalPage = (String)request.getAttribute(RequestDispatcher.FORWARD_SERVLET_PATH);
			if(originalPage == null) {
				if(
					request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH) != null
					&& (request instanceof HttpServletRequest)
				) {
					originalPage = ((HttpServletRequest)request).getServletPath();
				}
			}
		}
		return originalPage;
	}

	/**
	 * Sets the current request original page.
	 */
	public static void setOriginalPage(ServletRequest request, String page) {
		request.setAttribute(ORIGINAL_PAGE_REQUEST_ATTRIBUTE, page);
	}

	/**
	 * Gets the original page path corresponding to the original request before any forward/include.
	 * If no original page available, uses the servlet path from the provided request.
	 * 
	 * @see  #getOriginalPage(javax.servlet.ServletRequest)
	 * @see  RequestDispatcher#FORWARD_SERVLET_PATH
	 * @see  RequestDispatcher#INCLUDE_SERVLET_PATH
	 * @see  HttpServletRequest#getServletPath()
	 */
	public static String getOriginalPagePath(HttpServletRequest request) {
		String original = getOriginalPage(request);
		return (original != null) ? original : request.getServletPath();
	}

	/**
	 * Tracks the current dispatch page for correct page-relative paths.
	 */
	private static final String DISPATCHED_PAGE_REQUEST_ATTRIBUTE = Dispatcher.class.getName() + ".dispatchedPage";

	/**
	 * Gets the current request dispatched page or null if not set.
	 *
	 * @see  #getCurrentPagePath(javax.servlet.http.HttpServletRequest) for the version that uses current request as a default.
	 * @see  RequestDispatcher#INCLUDE_SERVLET_PATH
	 */
	public static String getDispatchedPage(ServletRequest request) {
		String dispatchedPage = (String)request.getAttribute(DISPATCHED_PAGE_REQUEST_ATTRIBUTE);
		if(dispatchedPage != null) {
			if(logger.isLoggable(Level.FINE)) logger.log(
				Level.FINE,
				"request={0}, dispatchedPage={1}",
				new Object[] {
					request,
					dispatchedPage
				}
			);
		} else {
			dispatchedPage = (String)request.getAttribute(RequestDispatcher.INCLUDE_SERVLET_PATH);
			if(dispatchedPage != null) {
				if(logger.isLoggable(Level.FINE)) logger.log(
					Level.FINE,
					"request={0}, " + RequestDispatcher.INCLUDE_SERVLET_PATH + "={1}",
					new Object[] {
						request,
						dispatchedPage
					}
				);
			}
		}
		return dispatchedPage;
	}

	/**
	 * Sets the current request dispatched page.
	 */
	public static void setDispatchedPage(ServletRequest request, String dispatchedPage) {
		if(logger.isLoggable(Level.FINE)) logger.log(
			Level.FINE,
			"request={0}, dispatchedPage={1}",
			new Object[] {
				request,
				dispatchedPage
			}
		);
		request.setAttribute(DISPATCHED_PAGE_REQUEST_ATTRIBUTE, dispatchedPage);
	}

	/**
	 * Gets the current page path, including any effects from include/forward.
	 * This will be the path of the current page on forward or include.
	 * This may be used as a substitute for HttpServletRequest.getServletPath() when the current page is needed instead of the originally requested servlet.
	 *
	 * @see  #getDispatchedPage(javax.servlet.ServletRequest)
	 * @see  RequestDispatcher#INCLUDE_SERVLET_PATH
	 */
	public static String getCurrentPagePath(HttpServletRequest request) {
		String dispatched = getDispatchedPage(request);
		if(dispatched != null) {
			if(logger.isLoggable(Level.FINE)) logger.log(
				Level.FINE,
				"request={0}, dispatched={1}",
				new Object[] {
					request,
					dispatched
				}
			);
			return dispatched;
		} else {
			String servletPath = request.getServletPath();
			if(logger.isLoggable(Level.FINE)) logger.log(
				Level.FINE,
				"request={0}. servletPath={1}",
				new Object[] {
					request,
					servletPath
				}
			);
			return servletPath;
		}
	}

	/**
	 * Performs a forward with the provided servlet path and associated dispatcher.
	 *
	 * @param  args  The arguments for the page, make unmodifiable and accessible as request-scope var "arg"
	 */
	public static void forward(
		String contextRelativePath,
		RequestDispatcher dispatcher,
		HttpServletRequest request,
		HttpServletResponse response,
		Map<String,?> args
	) throws ServletException, IOException {
		// Track original page when first accessed
		final String oldOriginal = getOriginalPage(request);
		try {
			// Set original request path if not already set
			if(oldOriginal == null) {
				setOriginalPage(request, request.getServletPath());
			}
			// Keep old dispatch page to restore
			final String oldDispatchPage = getDispatchedPage(request);
			try {
				// Store as new relative path source
				setDispatchedPage(request, contextRelativePath);
				// Keep old arguments to restore
				final Object oldArgs = request.getAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE);
				try {
					// Set new arguments
					request.setAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE,
						args==null ? null : AoCollections.optimalUnmodifiableMap(args)
					);
					// Perform dispatch
					dispatcher.forward(request, response);
				} finally {
					// Restore any previous args
					request.setAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE, oldArgs);
				}
			} finally {
				setDispatchedPage(request, oldDispatchPage);
			}
		} finally {
			if(oldOriginal == null) {
				setOriginalPage(request, null);
			}
		}
	}

	/**
	 * @see  #forward(java.lang.String, javax.servlet.RequestDispatcher, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void forward(
		String contextRelativePath,
		RequestDispatcher dispatcher,
		HttpServletRequest request,
		HttpServletResponse response
	) throws ServletException, IOException {
		forward(contextRelativePath, dispatcher, request, response, null);
	}

	/**
	 * Performs a forward, allowing page-relative paths and setting all values
	 * compatible with &lt;ao:forward&gt; tag.
	 *
	 * @param  args  The arguments for the page, make unmodifiable and accessible as request-scope var "arg"
	 *
	 * @see #forward(java.lang.String, javax.servlet.RequestDispatcher, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void forward(
		ServletContext servletContext,
		String page,
		HttpServletRequest request,
		HttpServletResponse response,
		Map<String,?> args
	) throws ServletException, IOException {
		// Resolve the dispatcher
		String contextRelativePath = URIResolver.getAbsolutePath(getCurrentPagePath(request), page);
		RequestDispatcher dispatcher = servletContext.getRequestDispatcher(contextRelativePath);
		if(dispatcher==null) throw new LocalizedServletException(accessor, "Dispatcher.dispatcherNotFound", contextRelativePath);
		forward(contextRelativePath, dispatcher, request, response, args);
	}

	/**
	 * @see  #forward(javax.servlet.ServletContext, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void forward(
		ServletContext servletContext,
		String page,
		HttpServletRequest request,
		HttpServletResponse response
	) throws ServletException, IOException {
		forward(servletContext, page, request, response, null);
	}

	/**
	 * Performs a forward with the provided servlet path and associated dispatcher.
	 *
	 * @param  args  The arguments for the page, make unmodifiable and accessible as request-scope var "arg"
	 * 
	 * @throws SkipPageException when the included page has been skipped due to a redirect.
	 */
	public static void include(
		String contextRelativePath,
		RequestDispatcher dispatcher,
		HttpServletRequest request,
		HttpServletResponse response,
		Map<String,?> args
	) throws SkipPageException, ServletException, IOException {
		// Track original page when first accessed
		final String oldOriginal = getOriginalPage(request);
		try {
			// Set original request path if not already set
			if(oldOriginal == null) {
				String servletPath = request.getServletPath();
				if(logger.isLoggable(Level.FINE)) logger.log(
					Level.FINE,
					"request={0}, servletPath={1}",
					new Object[] {
						request,
						servletPath
					}
				);
				setOriginalPage(request, servletPath);
			}
			// Keep old dispatch page to restore
			final String oldDispatchPage = getDispatchedPage(request);
			try {
				if(logger.isLoggable(Level.FINE)) logger.log(
					Level.FINE,
					"request={0}, oldDispatchPage={1}",
					new Object[] {
						request,
						oldDispatchPage
					}
				);
				// Store as new relative path source
				setDispatchedPage(request, contextRelativePath);
				// Keep old arguments to restore
				final Object oldArgs = request.getAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE);
				try {
					// Set new arguments
					request.setAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE,
						args==null ? null : AoCollections.optimalUnmodifiableMap(args)
					);
					// Perform dispatch
					Includer.dispatchInclude(dispatcher, request, response);
				} finally {
					// Restore any previous args
					request.setAttribute(Dispatcher.ARG_REQUEST_ATTRIBUTE, oldArgs);
				}
			} finally {
				setDispatchedPage(request, oldDispatchPage);
			}
		} finally {
			if(oldOriginal == null) {
				setOriginalPage(request, null);
			}
		}
	}

	/**
	 * @see  #include(java.lang.String, javax.servlet.RequestDispatcher, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void include(
		String contextRelativePath,
		RequestDispatcher dispatcher,
		HttpServletRequest request,
		HttpServletResponse response
	) throws SkipPageException, ServletException, IOException {
		include(contextRelativePath, dispatcher, request, response, null);
	}

	/**
	 * Performs an include, allowing page-relative paths and setting all values
	 * compatible with &lt;ao:include&gt; tag.
	 *
	 * @param  args  The arguments for the page, make unmodifiable and accessible as request-scope var "arg"
	 * 
	 * @throws SkipPageException when the included page has been skipped due to a redirect.
	 *
	 * @see  #include(java.lang.String, javax.servlet.RequestDispatcher, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void include(
		ServletContext servletContext,
		String page,
		HttpServletRequest request,
		HttpServletResponse response,
		Map<String,?> args
	) throws SkipPageException, ServletException, IOException {
		// Resolve the dispatcher
		String contextRelativePath;
		{
			String currentPagePath = getCurrentPagePath(request);
			if(logger.isLoggable(Level.FINE)) logger.log(
				Level.FINE,
				"request={0}, currentPagePath={1}",
				new Object[] {
					request,
					currentPagePath
				}
			);
			contextRelativePath = URIResolver.getAbsolutePath(currentPagePath, page);
			if(logger.isLoggable(Level.FINE)) logger.log(
				Level.FINE,
				"request={0}, contextRelativePath={1}",
				new Object[] {
					request,
					contextRelativePath
				}
			);
		}
		RequestDispatcher dispatcher = servletContext.getRequestDispatcher(contextRelativePath);
		if(dispatcher==null) throw new LocalizedServletException(accessor, "Dispatcher.dispatcherNotFound", contextRelativePath);
		include(contextRelativePath, dispatcher, request, response, args);
	}

	/**
	 * @see  #include(javax.servlet.ServletContext, java.lang.String, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.util.Map)
	 */
	public static void include(
		ServletContext servletContext,
		String page,
		HttpServletRequest request,
		HttpServletResponse response
	) throws SkipPageException, ServletException, IOException {
		include(servletContext, page, request, response, null);
	}
}
