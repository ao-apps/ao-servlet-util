/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019  AO Industries, Inc.
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

import com.aoindustries.io.Encoder;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.net.URIParameters;
import com.aoindustries.net.URIParametersUtils;
import com.aoindustries.net.URIResolver;
import com.aoindustries.servlet.ServletUtil;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;

/**
 * Static utilities that may be useful by servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 *
 * @see ServletUtil
 */
public class HttpServletUtil {

	private HttpServletUtil() {
	}

	private static final boolean DEBUG = false;

	/**
	 * @see  URIResolver#getAbsolutePath(java.lang.String, java.lang.String)
	 */
	public static String getAbsolutePath(HttpServletRequest request, String path) throws MalformedURLException {
		return URIResolver.getAbsolutePath(request.getServletPath(), path);
	}

	/**
	 * Determines if the requestor is Googlebot as described at:
	 * http://www.google.com/support/webmasters/bin/answer.py?answer=80553
	 */
	public static boolean isGooglebot(HttpServletRequest request) {
		@SuppressWarnings("unchecked")
		Enumeration<String> headers = request.getHeaders("User-Agent");
		while(headers.hasMoreElements()) {
			String userAgent = headers.nextElement();
			if(userAgent.contains("Googlebot")) {
				// Verify through reverse then forward DNS lookups
				String remoteAddr = request.getRemoteAddr();
				String remoteHost = request.getRemoteHost();
				try {
					InetAddress remoteIp = InetAddress.getByName(remoteAddr);
					// Do reverse lookup if container didn't do so
					if(remoteAddr.equals(remoteHost)) remoteHost = remoteIp.getCanonicalHostName();
					// Reverse DNS result must be in the googlebot.com domain
					if(remoteHost.endsWith(".googlebot.com") || remoteHost.endsWith(".googlebot.com.")) {
						// Forward DNS must resolve back to the original IP
						for(InetAddress actualIp : InetAddress.getAllByName(remoteHost)) {
							if(DEBUG) System.err.println("DEBUG: ServletUtil: Googlebot verified: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
							if(actualIp.equals(remoteIp)) return true;
						}
						if(DEBUG) System.err.println("DEBUG: ServletUtil: Googlebot agent with valid reverse DNS failed forward lookup: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
					}
					if(DEBUG) System.err.println("DEBUG: ServletUtil: Googlebot agent failed valid reverse DNS lookup: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
				} catch(UnknownHostException exception) {
					// Ignored
					if(DEBUG) System.err.println("DEBUG: ServletUtil: Googlebot agent verification failed due to exception: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\", exception=\""+exception+"\"");
				}
				break; // Only check the first Googlebot User-Agent header (there should normally only be one anyway)
			}
		}
		return false;
	}

	/**
	 * Gets an absolute URL for the given path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 *
	 * @param contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
	 */
	public static String getAbsoluteURL(HttpServletRequest request, String relPath, boolean contextRelative) {
		try {
			StringBuilder buffer = new StringBuilder();
			getAbsoluteURL(request, relPath, contextRelative, buffer);
			return buffer.toString();
		} catch(IOException e) {
			// Should never get IOException from StringBuilder.
			throw new RuntimeException(e);
		}
	}

	/**
	 * Gets an absolute URL for the given context-relative path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 */
	public static String getAbsoluteURL(HttpServletRequest request, String relPath) {
		return getAbsoluteURL(request, relPath, true);
	}

	/**
	 * Gets an absolute URL for the given path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 *
	 * @param contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
	 */
	public static void getAbsoluteURL(HttpServletRequest request, String relPath, boolean contextRelative, Appendable out) throws IOException {
		out.append(request.isSecure() ? "https://" : "http://");
		URIEncoder.encodeURI(request.getServerName(), out);
		int port = request.getServerPort();
		if(port!=(request.isSecure() ? 443 : 80)) out.append(':').append(Integer.toString(port));
		if(contextRelative) {
			URIEncoder.encodeURI(request.getContextPath(), out);
		}
		out.append(relPath);
	}

	/**
	 * Gets an absolute URL for the given context-relative path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 */
	public static void getAbsoluteURL(HttpServletRequest request, String relPath, Appendable out) throws IOException {
		getAbsoluteURL(request, relPath, true, out);
	}

	/**
	 * Gets an absolute URL for the given path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 *
	 * @param contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
	 */
	public static void getAbsoluteURL(HttpServletRequest request, String relPath, boolean contextRelative, Encoder encoder, Appendable out) throws IOException {
		if(encoder==null) {
			getAbsoluteURL(request, relPath, contextRelative, out);
		} else {
			encoder.append(request.isSecure() ? "https://" : "http://", out);
			URIEncoder.encodeURI(request.getServerName(), encoder, out);
			int port = request.getServerPort();
			if(port!=(request.isSecure() ? 443 : 80)) encoder.append(':', out).append(Integer.toString(port), out);
			if(contextRelative) {
				URIEncoder.encodeURI(request.getContextPath(), encoder, out);
			}
			encoder.append(relPath, out);
		}
	}

	/**
	 * Gets an absolute URL for the given context-relative path.  This includes
	 * protocol, port, context path, and relative path.
	 * No URL rewriting is performed.
	 */
	public static void getAbsoluteURL(HttpServletRequest request, String relPath, Encoder encoder, Appendable out) throws IOException {
		getAbsoluteURL(request, relPath, true, encoder, out);
	}

	/**
	 * Gets the absolute URL that should be used for a redirect.
	 * 
	 * @param  href  The absolute, context-relative, or page-relative path to redirect to.
	 *               The following actions are performed on the provided href:
	 *               <ol>
	 *                 <li>Convert page-relative paths to context-relative path, resolving ./ and ../</li>
	 *                 <li>Encode URI to ASCII format via {@link URIEncoder#encodeURI(java.lang.String)}</li>
	 *                 <li>Convert to absolute URL if needed.  This will also add the context path.</li>
	 *                 <li>Perform URL rewriting {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}</li>
	 *               </ol>
	 *
	 * @param  canonical The value to use for {@link Canonical} during {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}
	 *
	 * @see  #sendRedirect(javax.servlet.http.HttpServletResponse, java.lang.String, int)
	 */
	@SuppressWarnings("try")
	public static String getRedirectLocation(
		HttpServletRequest request,
		HttpServletResponse response,
		String servletPath,
		String href,
		boolean canonical
	) throws MalformedURLException {
		// Convert page-relative paths to context-relative path, resolving ./ and ../
		href = URIResolver.getAbsolutePath(servletPath, href);

		// Encode URI to ASCII format
		href = URIEncoder.encodeURI(href);

		// Convert to absolute URL if needed.  This will also add the context path.
		if(href.startsWith("/")) href = getAbsoluteURL(request, href);

		// Perform URL rewriting
		try (Canonical c = Canonical.set(canonical)) {
			href = response.encodeRedirectURL(href);
		}

		return href;
	}

	/**
	 * Sends a redirect to the provided absolute URL location.
	 * Encodes the location to US-ASCII format.
	 *
	 * @see  #getRedirectLocation(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String) 
	 */
	public static void sendRedirect(
		HttpServletResponse response,
		String location,
		int status
	) throws IllegalStateException, IOException {
		// Response must not be committed
		if(response.isCommitted()) throw new IllegalStateException("Unable to redirect: Response already committed");

		response.setHeader("Location", URIEncoder.encodeURI(location));
		response.sendError(status);
	}

	/**
	 * Sends a redirect with relative paths determined from the request servlet path.
	 * 
	 * @see  #getRedirectLocation(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String)  for transformations applied to the href
	 */
	public static void sendRedirect(
		HttpServletRequest request,
		HttpServletResponse response,
		String href,
		boolean canonical,
		int status
	) throws IllegalStateException, IOException {
		sendRedirect(
			response,
			getRedirectLocation(
				request,
				response,
				request.getServletPath(),
				href,
				canonical
			),
			status
		);
	}

	/**
	 * Gets the current request URI in context-relative form.  The contextPath stripped.
	 */
	public static String getContextRequestUri(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = URIEncoder.encodeURI(request.getContextPath());
		int cpLen = contextPath.length();
		if(cpLen > 0) {
			assert requestUri.startsWith(contextPath);
			return requestUri.substring(cpLen);
		} else {
			return requestUri;
		}
	}

	public static final String METHOD_DELETE = "DELETE";
	public static final String METHOD_HEAD = "HEAD";
	public static final String METHOD_GET = "GET";
	public static final String METHOD_OPTIONS = "OPTIONS";
	public static final String METHOD_POST = "POST";
	public static final String METHOD_PUT = "PUT";
	public static final String METHOD_TRACE = "TRACE";

	public static Method[] getAllDeclaredMethods(Class<?> stopClass, Class<?> c) {
		if (c.equals(stopClass)) {
			return null;
		}
		Method[] parentMethods = getAllDeclaredMethods(stopClass, c.getSuperclass());
		Method[] thisMethods = c.getDeclaredMethods();
		if ((parentMethods != null) && (parentMethods.length > 0)) {
			Method[] allMethods = new Method[parentMethods.length + thisMethods.length];
			System.arraycopy(
				parentMethods, 0,
				allMethods, 0,
				parentMethods.length
			);
			System.arraycopy(
				thisMethods, 0,
				allMethods, parentMethods.length,
				thisMethods.length
			);
			thisMethods = allMethods;
		}
		return thisMethods;
	}

	/**
	 * A reusable doOptions implementation for servlets.
	 */
	public static <S extends HttpServlet> void doOptions(
		HttpServletResponse response,
		Class<S> stopClass,
		Class<? extends S> thisClass,
		String doGet,
		String doPost,
		String doPut,
		String doDelete,
		Class<?>[] paramTypes
	) {
		boolean ALLOW_GET = false;
		boolean ALLOW_HEAD = false;
		boolean ALLOW_POST = false;
		boolean ALLOW_PUT = false;
		boolean ALLOW_DELETE = false;
		boolean ALLOW_TRACE = true;
		boolean ALLOW_OPTIONS = true;
		for (
			Method method
			: getAllDeclaredMethods(stopClass, thisClass)
		) {
			if(Arrays.equals(paramTypes, method.getParameterTypes())) {
				String methodName = method.getName();
				if (doGet.equals(methodName)) {
					ALLOW_GET = true;
					ALLOW_HEAD = true;
				} else if (doPost.equals(methodName)) {
					ALLOW_POST = true;
				} else if (doPut.equals(methodName)) {
					ALLOW_PUT = true;
				} else if (doDelete.equals(methodName)) {
					ALLOW_DELETE = true;
				}
			}
		}
		StringBuilder allow = new StringBuilder();
		if (ALLOW_GET) {
			// if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_GET);
		}
		if (ALLOW_HEAD) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_HEAD);
		}
		if (ALLOW_POST) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_POST);
		}
		if (ALLOW_PUT) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_PUT);
		}
		if (ALLOW_DELETE) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_DELETE);
		}
		if (ALLOW_TRACE) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_TRACE);
		}
		if (ALLOW_OPTIONS) {
			if(allow.length() != 0) allow.append(", ");
			allow.append(METHOD_OPTIONS);
		}
		response.setHeader("Allow", allow.toString());
	}

	/**
	 * Performs all the proper URL conversions along with optionally adding a lastModified parameter.
	 * This includes:
	 * <ol>
	 *   <li>Converting any page-relative path to a context-relative path starting with a slash (/)</li>
	 *   <li>Adding any additional parameters</li>
	 *   <li>Optionally adding lastModified parameter</li>
	 *   <li>Converting any context-relative path to a site-relative path by prefixing contextPath</li>
	 *   <li>Encoding any URL path characters not defined in <a href="https://tools.ietf.org/html/rfc3986#section-2.2">RFC 3986: Reserved Characters</a></li>
	 *   <li>Rewrite with {@link HttpServletResponse#encodeURL(java.lang.String)}</li>
	 *   <li>Optionally convert to an absolute URL: <code>http(s)://…</code></li>
	 * </ol>
	 *
	 * @param  canonical The value to use for {@link Canonical} during {@link HttpServletResponse#encodeURL(java.lang.String)}
	 */
	@SuppressWarnings("try")
	public static String buildUrl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String url,
		URIParameters params,
		boolean urlAbsolute,
		boolean canonical,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		String servletPath = Dispatcher.getCurrentPagePath(request);
		url = URIResolver.getAbsolutePath(servletPath, url);
		url = URIParametersUtils.addParams(url, params);
		url = LastModifiedServlet.addLastModified(servletContext, request, servletPath, url, addLastModified);
		url = URIEncoder.encodeURI(url);
		if(url.startsWith("/")) {
			if(urlAbsolute) {
				url = getAbsoluteURL(request, url);
			} else {
				String contextPath = request.getContextPath();
				if(!contextPath.isEmpty()) {
					url = URIEncoder.encodeURI(contextPath) + url;
				}
			}
		}
		try (Canonical c = Canonical.set(canonical)) {
			url = response.encodeURL(url);
		}
		return url;
	}

	/**
	 * @see #buildUrl(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, boolean, com.aoindustries.servlet.http.LastModifiedServlet.AddLastModifiedWhen)
	 */
	public static String buildUrl(
		PageContext pageContext,
		String url,
		URIParameters params,
		boolean urlAbsolute,
		boolean canonical,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		return buildUrl(
			pageContext.getServletContext(),
			(HttpServletRequest)pageContext.getRequest(),
			(HttpServletResponse)pageContext.getResponse(),
			url,
			params,
			urlAbsolute,
			canonical,
			addLastModified
		);
	}

	/**
	 * @see #buildUrl(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, boolean, com.aoindustries.servlet.http.LastModifiedServlet.AddLastModifiedWhen)
	 */
	public static String buildUrl(
		JspContext jspContext,
		String url,
		URIParameters params,
		boolean srcAbsolute,
		boolean canonical,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		return buildUrl(
			(PageContext)jspContext,
			url,
			params,
			srcAbsolute,
			canonical,
			addLastModified
		);
	}
}
