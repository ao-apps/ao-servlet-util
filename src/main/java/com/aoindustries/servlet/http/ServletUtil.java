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
import com.aoindustries.lang.NullArgumentException;
import com.aoindustries.net.URIDecoder;
import com.aoindustries.net.URIParameters;
import com.aoindustries.net.URIParametersUtils;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.net.URIParser;
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.SkipPageException;

/**
 * Static utilities that may be useful by servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 */
public class ServletUtil {

	private ServletUtil() {
	}

	private static final boolean DEBUG = false;

	/**
	 * A shared {@link SkipPageException} instance to avoid exception creation overhead
	 * for the routine operation of skipping pages.
	 */
	public static final SkipPageException SKIP_PAGE_EXCEPTION = new SkipPageException() {
		private static final long serialVersionUID = 1L;

		// Hides any stack trace from original caller that first instantiated the object.
		{
			StackTraceElement[] stackTrace = getStackTrace();
			if(stackTrace != null && stackTrace.length > 1) {
				setStackTrace(
					new StackTraceElement[] {
						stackTrace[0]
					}
				);
			}
		}
	};

	private static final String DEFAULT_REQUEST_ENCODING = "ISO-8859-1";

	/**
	 * Gets the request encoding or ISO-8859-1 when not available.
	 */
	public static String getRequestEncoding(ServletRequest request) {
		String requestEncoding = request.getCharacterEncoding();
		return requestEncoding != null ? requestEncoding : DEFAULT_REQUEST_ENCODING;
	}

	/**
	 * Converts a possibly-relative path to a context-relative absolute path.
	 * Resolves ./ and ../ at the beginning of the URL but not in the middle of the URL.
	 * If the URL begins with http:, https:, file:, javascript:, mailto:, telnet:, tel:, or cid:, (case-insensitive) it is not altered.
	 *
	 * @param  servletPath  Required when path might be altered.
	 */
	public static String getAbsolutePath(String servletPath, String relativeUrlPath) throws MalformedURLException {
		char firstChar;
		if(
			relativeUrlPath.length() > 0
			&& (firstChar=relativeUrlPath.charAt(0)) != '/'
			&& firstChar != '#' // Skip anchor-only paths
			&& !URIParser.isScheme(relativeUrlPath, "http")
			&& !URIParser.isScheme(relativeUrlPath, "https")
			&& !URIParser.isScheme(relativeUrlPath, "file")
			&& !URIParser.isScheme(relativeUrlPath, "javascript")
			&& !URIParser.isScheme(relativeUrlPath, "mailto")
			&& !URIParser.isScheme(relativeUrlPath, "telnet")
			&& !URIParser.isScheme(relativeUrlPath, "tel")
			&& !URIParser.isScheme(relativeUrlPath, "cid")
		) {
			NullArgumentException.checkNotNull(servletPath, "servletPath");
			int slashPos = servletPath.lastIndexOf('/');
			if(slashPos==-1) throw new MalformedURLException("No slash found in servlet path: "+servletPath);
			final String newPath = relativeUrlPath;
			final int newPathLen = newPath.length();
			int newPathStart = 0;
			boolean modified;
			do {
				modified = false;
				if(
					newPathLen >= (newPathStart+2)
					&& newPath.regionMatches(newPathStart, "./", 0, 2)
				) {
					newPathStart += 2;
					modified = true;
				}
				if(
					newPathLen >= (newPathStart+3)
					&& newPath.regionMatches(newPathStart, "../", 0, 3)
				) {
					slashPos = servletPath.lastIndexOf('/', slashPos-1);
					if(slashPos==-1) throw new MalformedURLException("Too many ../ in relativeUrlPath: "+relativeUrlPath);

					newPathStart += 3;
					modified = true;
				}
			} while(modified);
			relativeUrlPath =
				new StringBuilder((slashPos+1) + (newPathLen-newPathStart))
				.append(servletPath, 0, slashPos+1)
				.append(newPath, newPathStart, newPathLen)
				.toString();
		}
		return relativeUrlPath;
	}

	/**
	 * @see  #getAbsolutePath(java.lang.String, java.lang.String)
	 */
	public static String getAbsolutePath(HttpServletRequest request, String path) throws MalformedURLException {
		return getAbsolutePath(request.getServletPath(), path);
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
		out.append(request.getServerName());
		int port = request.getServerPort();
		if(port!=(request.isSecure() ? 443 : 80)) out.append(':').append(Integer.toString(port));
		if(contextRelative) out.append(request.getContextPath());
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
			encoder.append(request.getServerName(), out);
			int port = request.getServerPort();
			if(port!=(request.isSecure() ? 443 : 80)) encoder.append(':', out).append(Integer.toString(port), out);
			if(contextRelative) encoder.append(request.getContextPath(), out);
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
	 *                 <li>Encode URI to ASCII format via {@link #encodeURI(java.lang.String, javax.servlet.ServletResponse)}</li>
	 *                 <li>Perform URL rewriting {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}</li>
	 *                 <li>Convert to absolute URL if needed.  This will also add the context path.</li>
	 *               </ol>
	 *
	 * @see  #sendRedirect(javax.servlet.http.HttpServletResponse, java.lang.String, int)
	 */
	public static String getRedirectLocation(
		HttpServletRequest request,
		HttpServletResponse response,
		String servletPath,
		String href
	) throws MalformedURLException {
		// Convert page-relative paths to context-relative path, resolving ./ and ../
		href = getAbsolutePath(servletPath, href);

		// Encode URI to ASCII format
		href = encodeURI(href, response);

		// Perform URL rewriting
		href = response.encodeRedirectURL(href);

		// Convert to absolute URL if needed.  This will also add the context path.
		if(href.startsWith("/")) href = getAbsoluteURL(request, href);

		return href;
	}

	/**
	 * Sends a redirect to the provided absolute URL location.
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

		response.setHeader("Location", location);
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
		int status
	) throws IllegalStateException, IOException {
		sendRedirect(
			response,
			getRedirectLocation(
				request,
				response,
				request.getServletPath(),
				href
			),
			status
		);
	}

	/**
	 * Gets the current request URI in context-relative form.  The contextPath stripped.
	 *
	 * @deprecated  TODO: getContextPath sometimes comes back percent encoded, but is usually decoded.  Tomcat 7-only issue?
	 *                    Deal with this here, and all other places where contextPath used for comparison.
	 *                    Suggest encoding both then comparing.
	 */
	@Deprecated
	public static String getContextRequestUri(HttpServletRequest request) {
		String requestUri = request.getRequestURI();
		String contextPath = request.getContextPath();
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
	 */
	public static String buildUrl(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		String url,
		URIParameters params,
		boolean urlAbsolute,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			String servletPath = Dispatcher.getCurrentPagePath(request);
			url = ServletUtil.getAbsolutePath(servletPath, url);
			url = URIParametersUtils.addParams(url, params, responseEncoding);
			url = LastModifiedServlet.addLastModified(servletContext, request, servletPath, url, addLastModified);
			if(!urlAbsolute && url.startsWith("/")) {
				String contextPath = request.getContextPath();
				if(!contextPath.isEmpty()) url = contextPath + url;
			}
			url = response.encodeURL(URIEncoder.encodeURI(url, responseEncoding));
			if(urlAbsolute && url.startsWith("/")) url = ServletUtil.getAbsoluteURL(request, url);
			return url;
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see #buildUrl(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, com.aoindustries.servlet.http.LastModifiedServlet.AddLastModifiedWhen)
	 */
	public static String buildUrl(
		PageContext pageContext,
		String url,
		URIParameters params,
		boolean urlAbsolute,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		return buildUrl(
			pageContext.getServletContext(),
			(HttpServletRequest)pageContext.getRequest(),
			(HttpServletResponse)pageContext.getResponse(),
			url,
			params,
			urlAbsolute,
			addLastModified
		);
	}

	/**
	 * @see #buildUrl(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoindustries.net.URIParameters, boolean, com.aoindustries.servlet.http.LastModifiedServlet.AddLastModifiedWhen)
	 */
	public static String buildUrl(
		JspContext jspContext,
		String url,
		URIParameters params,
		boolean srcAbsolute,
		LastModifiedServlet.AddLastModifiedWhen addLastModified
	) throws MalformedURLException {
		return buildUrl(
			(PageContext)jspContext,
			url,
			params,
			srcAbsolute,
			addLastModified
		);
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static String encodeURIComponent(String uri, ServletResponse response) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			return URIEncoder.encodeURIComponent(uri, responseEncoding);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURIComponent(String uri, ServletResponse response, Appendable out) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURIComponent(uri, responseEncoding, out);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURIComponent(String uri, ServletResponse response, Appendable out, Encoder encoder) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURIComponent(uri, responseEncoding, out, encoder);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURIComponent(String uri, ServletResponse response, StringBuilder sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURIComponent(uri, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURIComponent(String uri, ServletResponse response, StringBuffer sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURIComponent(uri, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static String decodeURIComponent(String uri, ServletRequest request) throws UnsupportedEncodingException {
		return URIDecoder.decodeURIComponent(uri, getRequestEncoding(request));
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURIComponent(String uri, ServletRequest request, Appendable out) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), out);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURIComponent(String uri, ServletRequest request, Appendable out, Encoder encoder) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), out, encoder);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURIComponent(String uri, ServletRequest request, StringBuilder sb) throws UnsupportedEncodingException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), sb);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURIComponent(String uri, ServletRequest request, StringBuffer sb) throws UnsupportedEncodingException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), sb);
	}

	/**
	 * @see  URIEncoder#encodeURI(java.lang.String, java.lang.String)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static String encodeURI(String uri, ServletResponse response) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			return URIEncoder.encodeURI(uri, responseEncoding);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURI(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURI(String uri, ServletResponse response, Appendable out) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURI(uri, responseEncoding, out);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURI(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURI(String uri, ServletResponse response, Appendable out, Encoder encoder) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURI(uri, responseEncoding, out, encoder);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURI(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURI(String uri, ServletResponse response, StringBuilder sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURI(uri, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIEncoder#encodeURI(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public static void encodeURI(String uri, ServletResponse response, StringBuffer sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			URIEncoder.encodeURI(uri, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  URIDecoder#decodeURI(java.lang.String, java.lang.String)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static String decodeURI(String uri, ServletRequest request) throws UnsupportedEncodingException {
		return URIDecoder.decodeURI(uri, getRequestEncoding(request));
	}

	/**
	 * @see  URIDecoder#decodeURI(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURI(String uri, ServletRequest request, Appendable out) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURI(uri, getRequestEncoding(request), out);
	}

	/**
	 * @see  URIDecoder#decodeURI(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURI(String uri, ServletRequest request, Appendable out, Encoder encoder) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURI(uri, getRequestEncoding(request), out, encoder);
	}

	/**
	 * @see  URIDecoder#decodeURI(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURI(String uri, ServletRequest request, StringBuilder sb) throws UnsupportedEncodingException {
		URIDecoder.decodeURI(uri, getRequestEncoding(request), sb);
	}

	/**
	 * @see  URIDecoder#decodeURI(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public static void decodeURI(String uri, ServletRequest request, StringBuffer sb) throws UnsupportedEncodingException {
		URIDecoder.decodeURI(uri, getRequestEncoding(request), sb);
	}
}
