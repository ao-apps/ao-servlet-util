/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2009, 2010, 2011, 2012, 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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
 * along with ao-servlet-util.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.aoapps.servlet.http;

import com.aoapps.lang.io.Encoder;
import com.aoapps.net.URIEncoder;
import com.aoapps.net.URIParameters;
import com.aoapps.net.URIParametersUtils;
import com.aoapps.net.URIResolver;
import com.aoapps.servlet.ServletUtil;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;

/**
 * Static utilities that may be useful by servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 *
 * @see ServletUtil
 */
public final class HttpServletUtil {

  /** Make no instances. */
  private HttpServletUtil() {
    throw new AssertionError();
  }

  private static final Logger logger = Logger.getLogger(HttpServletUtil.class.getName());

  /**
   * Determines if the requestor is Googlebot as described at:
   * http://www.google.com/support/webmasters/bin/answer.py?answer=80553
   */
  public static boolean isGooglebot(HttpServletRequest request) {
    @SuppressWarnings("unchecked")
    Enumeration<String> headers = request.getHeaders("User-Agent");
    while (headers.hasMoreElements()) {
      String userAgent = headers.nextElement();
      if (userAgent.contains("Googlebot")) {
        // Verify through reverse then forward DNS lookups
        String remoteAddr = request.getRemoteAddr();
        String remoteHost = request.getRemoteHost();
        try {
          InetAddress remoteIp = InetAddress.getByName(remoteAddr);
          // Do reverse lookup if container didn't do so
          if (remoteAddr.equals(remoteHost)) {
            remoteHost = remoteIp.getCanonicalHostName();
          }
          // Reverse DNS result must be in the googlebot.com domain
          if (remoteHost.endsWith(".googlebot.com") || remoteHost.endsWith(".googlebot.com.")) {
            // Forward DNS must resolve back to the original IP
            for (InetAddress actualIp : InetAddress.getAllByName(remoteHost)) {
              if (logger.isLoggable(Level.FINER)) {
                logger.finer("DEBUG: ServletUtil: Googlebot verified: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
              }
              if (actualIp.equals(remoteIp)) {
                return true;
              }
            }
            if (logger.isLoggable(Level.FINE)) {
              logger.fine("DEBUG: ServletUtil: Googlebot agent with valid reverse DNS failed forward lookup: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
            }
          }
          if (logger.isLoggable(Level.FINE)) {
            logger.fine("DEBUG: ServletUtil: Googlebot agent failed valid reverse DNS lookup: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"");
          }
        } catch (UnknownHostException exception) {
          // Ignored
          if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "DEBUG: ServletUtil: Googlebot agent verification failed due to exception: userAgent=\""+userAgent+"\", remoteAddr=\""+remoteAddr+"\", remoteHost=\""+remoteHost+"\"", exception);
          }
        }
        break; // Only check the first Googlebot User-Agent header (there should normally only be one anyway)
      }
    }
    return false;
  }

  /**
   * Resolves a possibly page-relative path to a context-absolute path.
   *
   * @param  path  The absolute URL, context-absolute path, or page-relative path
   *
   * @see  Dispatcher#getCurrentPagePath(javax.servlet.http.HttpServletRequest)
   * @see  URIResolver#getAbsolutePath(java.lang.String, java.lang.String)
   */
  public static String getAbsolutePath(HttpServletRequest request, String path) throws MalformedURLException {
    return URIResolver.getAbsolutePath(
      Dispatcher.getCurrentPagePath(request),
      path
    );
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path (optional), and path.
   * No URL rewriting is performed.
   *
   * @param  contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static String getAbsoluteURL(HttpServletRequest request, boolean contextRelative, String path) {
    try {
      StringBuilder buffer = new StringBuilder();
      getAbsoluteURL(request, contextRelative, path, buffer);
      return buffer.toString();
    } catch (IOException e) {
      throw new AssertionError("Should never get IOException from StringBuilder.", e);
    }
  }

  /**
   * @deprecated  Please use {@link #getAbsoluteURL(javax.servlet.http.HttpServletRequest, boolean, java.lang.String)} instead.
   */
  @Deprecated
  public static String getAbsoluteURL(HttpServletRequest request, String path, boolean contextRelative) {
    return getAbsoluteURL(request, contextRelative, path);
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path, and path.
   * No URL rewriting is performed.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static String getAbsoluteURL(HttpServletRequest request, String path) {
    return getAbsoluteURL(request, true, path);
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path (optional), and path.
   * No URL rewriting is performed.
   *
   * @param  contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static void getAbsoluteURL(HttpServletRequest request, boolean contextRelative, String path, Appendable out) throws IOException {
    // TODO: Use request.getScheme() instead?
    out.append(request.isSecure() ? "https://" : "http://");
    URIEncoder.encodeURI(request.getServerName(), out);
    int port = request.getServerPort();
    if (port != (request.isSecure() ? 443 : 80)) {
      out.append(':').append(Integer.toString(port));
    }
    if (contextRelative) {
      URIEncoder.encodeURI(request.getContextPath(), out);
    }
    out.append(path);
  }

  /**
   * @deprecated  Please use {@link #getAbsoluteURL(javax.servlet.http.HttpServletRequest, boolean, java.lang.String, java.lang.Appendable)} instead.
   */
  @Deprecated
  public static void getAbsoluteURL(HttpServletRequest request, String path, boolean contextRelative, Appendable out) throws IOException {
    getAbsoluteURL(request, contextRelative, path, out);
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path, and path.
   * No URL rewriting is performed.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static void getAbsoluteURL(HttpServletRequest request, String path, Appendable out) throws IOException {
    getAbsoluteURL(request, true, path, out);
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path (optional), and path.
   * No URL rewriting is performed.
   *
   * @param  contextRelative  When {@code true}, includes {@link HttpServletRequest#getContextPath()} in the URL.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static void getAbsoluteURL(HttpServletRequest request, boolean contextRelative, String path, Encoder encoder, Appendable out) throws IOException {
    if (encoder == null) {
      getAbsoluteURL(request, contextRelative, path, out);
    } else {
      // TODO: Use request.getScheme() instead?
      encoder.append(request.isSecure() ? "https://" : "http://", out);
      URIEncoder.encodeURI(request.getServerName(), encoder, out);
      int port = request.getServerPort();
      if (port != (request.isSecure() ? 443 : 80)) {
        encoder.append(':', out).append(Integer.toString(port), out);
      }
      if (contextRelative) {
        URIEncoder.encodeURI(request.getContextPath(), encoder, out);
      }
      encoder.append(path, out);
    }
  }

  /**
   * @deprecated  Please use {@link #getAbsoluteURL(javax.servlet.http.HttpServletRequest, boolean, java.lang.String, com.aoapps.lang.io.Encoder, java.lang.Appendable)} instead.
   */
  @Deprecated
  public static void getAbsoluteURL(HttpServletRequest request, String path, boolean contextRelative, Encoder encoder, Appendable out) throws IOException {
    getAbsoluteURL(request, contextRelative, path, encoder, out);
  }

  /**
   * Gets an absolute URL for the given path.  This includes
   * protocol, port, context path, and path.
   * No URL rewriting is performed.
   *
   * @param  path  The path appended to the URL verbatim.  To support page-relative paths, first use
   *               {@link #getAbsolutePath(javax.servlet.http.HttpServletRequest, java.lang.String)}
   */
  public static void getAbsoluteURL(HttpServletRequest request, String path, Encoder encoder, Appendable out) throws IOException {
    getAbsoluteURL(request, true, path, encoder, out);
  }

  @FunctionalInterface
  public static interface UrlModifier {
    /**
     * Modifies the URL that is being processed.  This is called
     * after the parameters are added, and before URI encoding.
     */
    String modify(String url) throws MalformedURLException;
  }

  /**
   * Builds a URL that should be used for a redirect location,
   * including all the proper URL conversions.  This includes:
   * <ol>
   *   <li>Converting a page-relative path to a context-absolute path starting with a slash (/), resolving ./ and ../</li>
   *   <li>Adding any additional parameters</li>
   *   <li>Optionally {@linkplain UrlModifier modifying the URL}</li>
   *   <li>Encoding any URL path characters not defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-2.2">RFC 3986: Reserved Characters</a></li>
   *   <li>Converting any context-absolute path to a site-absolute path by prefixing {@linkplain HttpServletRequest#getContextPath() contextPath}</li>
   *   <li>Optionally convert to an absolute URL: <code>http[s]://…</code></li>
   *   <li>Rewrite with {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}</li>
   *   <li>Final US-ASCII encoding since Location must always be <a href="https://datatracker.ietf.org/doc/html/rfc3986">RFC 3986</a></li>
   * </ol>
   *
   * @param  href  The absolute URL, context-absolute path, or page-relative path
   *
   * @param  canonical The value to use for {@link Canonical} during {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}
   *
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  @SuppressWarnings("try")
  public static String buildRedirectURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    href = URIResolver.getAbsolutePath(servletPath, href);
    href = URIParametersUtils.addParams(href, params);
    if (modifier != null) {
      href = modifier.modify(href);
    }
    href = URIEncoder.encodeURI(href);
    if (href.startsWith("/")) {
      if (absolute) {
        href = getAbsoluteURL(request, true, href);
      } else {
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
          href = URIEncoder.encodeURI(contextPath) + href;
        }
      }
    }
    try (Canonical c = Canonical.set(canonical)) {
      href = response.encodeRedirectURL(href);
    }
    href = URIEncoder.encodeURI(href);
    return href;
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      request,
      response,
      servletPath,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * Builds a URL that should be used for a redirect location,
   * with path resolved relative to the given request.
   *
   * @see  Dispatcher#getCurrentPagePath(javax.servlet.http.HttpServletRequest)
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      request,
      response,
      Dispatcher.getCurrentPagePath(request),
      href,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      request,
      response,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    PageContext pageContext,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      (HttpServletRequest)pageContext.getRequest(),
      (HttpServletResponse)pageContext.getResponse(),
      href,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    PageContext pageContext,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      pageContext,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.jsp.JspContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    JspContext jspContext,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      (PageContext)jspContext,
      href,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.jsp.JspContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildRedirectURL(
    JspContext jspContext,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildRedirectURL(
      jspContext,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * Builds a URL with all the proper URL conversions.  This includes:
   * <ol>
   *   <li>Converting a page-relative path to a context-absolute path starting with a slash (/), resolving ./ and ../</li>
   *   <li>Adding any additional parameters</li>
   *   <li>Optionally {@linkplain UrlModifier modifying the URL}</li>
   *   <li>Encoding any URL path characters not defined in <a href="https://datatracker.ietf.org/doc/html/rfc3986#section-2.2">RFC 3986: Reserved Characters</a></li>
   *   <li>Converting any context-absolute path to a site-absolute path by prefixing {@linkplain HttpServletRequest#getContextPath() contextPath}</li>
   *   <li>Optionally convert to an absolute URL: <code>http[s]://…</code></li>
   *   <li>Rewrite with {@link HttpServletResponse#encodeURL(java.lang.String)}</li>
   * </ol>
   *
   * @param  url  The absolute URL, context-absolute path, or page-relative path
   *
   * @param  canonical The value to use for {@link Canonical} during {@link HttpServletResponse#encodeURL(java.lang.String)}
   */
  @SuppressWarnings("try")
  public static String buildURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String url,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    url = URIResolver.getAbsolutePath(servletPath, url);
    url = URIParametersUtils.addParams(url, params);
    if (modifier != null) {
      url = modifier.modify(url);
    }
    url = URIEncoder.encodeURI(url);
    if (url.startsWith("/")) {
      if (absolute) {
        url = getAbsoluteURL(request, true, url);
      } else {
        String contextPath = request.getContextPath();
        if (!contextPath.isEmpty()) {
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
   * @see  #buildURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String url,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      request,
      response,
      servletPath,
      url,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * Builds a URL with path resolved relative to the given request.
   *
   * @see  Dispatcher#getCurrentPagePath(javax.servlet.http.HttpServletRequest)
   * @see  #buildURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String url,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      request,
      response,
      Dispatcher.getCurrentPagePath(request),
      url,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    HttpServletRequest request,
    HttpServletResponse response,
    String url,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      request,
      response,
      url,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    PageContext pageContext,
    String url,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      (HttpServletRequest)pageContext.getRequest(),
      (HttpServletResponse)pageContext.getResponse(),
      url,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildURL(javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    PageContext pageContext,
    String url,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      pageContext,
      url,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildURL(javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    JspContext jspContext,
    String url,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      (PageContext)jspContext,
      url,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildURL(javax.servlet.jsp.JspContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static String buildURL(
    JspContext jspContext,
    String url,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException {
    return buildURL(
      jspContext,
      url,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * Sends a redirect to the provided location.
   * Encodes the location to US-ASCII format.
   * Response must not be {@linkplain HttpServletResponse#isCommitted() committed}.
   *
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   *
   * @throws  IllegalStateException  when the response is already {@linkplain HttpServletResponse#isCommitted() committed}
   */
  public static void sendRedirect(
    int status,
    HttpServletResponse response,
    String location
  ) throws IllegalStateException, IOException {
    // Response must not be committed
    if (response.isCommitted()) {
      throw new IllegalStateException("Unable to redirect: Response already committed");
    }

    response.setHeader("Location", URIEncoder.encodeURI(location));
    response.sendError(status);
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletResponse, java.lang.String)
   *
   * @throws  IllegalStateException  when the response is already {@linkplain HttpServletResponse#isCommitted() committed}
   */
  public static void sendRedirect(
    int status,
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      response,
      buildRedirectURL(
        request,
        response,
        servletPath,
        href,
        params,
        modifier,
        absolute,
        canonical
      )
    );
  }

  /**
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static void sendRedirect(
    int status,
    HttpServletRequest request,
    HttpServletResponse response,
    String servletPath,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      request,
      response,
      servletPath,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletResponse, java.lang.String)
   *
   * @throws  IllegalStateException  when the response is already {@linkplain HttpServletResponse#isCommitted() committed}
   */
  public static void sendRedirect(
    int status,
    HttpServletRequest request,
    HttpServletResponse response,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      response,
      buildRedirectURL(
        request,
        response,
        href,
        params,
        modifier,
        absolute,
        canonical
      )
    );
  }

  /**
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static void sendRedirect(
    int status,
    HttpServletRequest request,
    HttpServletResponse response,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      request,
      response,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.http.HttpServletResponse, java.lang.String)
   *
   * @throws  IllegalStateException  when the response is already {@linkplain HttpServletResponse#isCommitted() committed}
   */
  public static void sendRedirect(
    int status,
    PageContext pageContext,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      (HttpServletResponse)pageContext.getResponse(),
      buildRedirectURL(
        pageContext,
        href,
        params,
        modifier,
        absolute,
        canonical
      )
    );
  }

  /**
   * @see  #sendRedirect(int, javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static void sendRedirect(
    int status,
    PageContext pageContext,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      pageContext,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * @see  #buildRedirectURL(javax.servlet.jsp.JspContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   * @see  #sendRedirect(int, javax.servlet.jsp.PageContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   *
   * @throws  IllegalStateException  when the response is already {@linkplain HttpServletResponse#isCommitted() committed}
   */
  public static void sendRedirect(
    int status,
    JspContext jspContext,
    String href,
    URIParameters params,
    UrlModifier modifier,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      (PageContext)jspContext,
      href,
      params,
      modifier,
      absolute,
      canonical
    );
  }

  /**
   * @see  #sendRedirect(int, javax.servlet.jsp.JspContext, java.lang.String, com.aoapps.net.URIParameters, com.aoapps.servlet.http.HttpServletUtil.UrlModifier, boolean, boolean)
   */
  public static void sendRedirect(
    int status,
    JspContext jspContext,
    String href,
    URIParameters params,
    boolean absolute,
    boolean canonical
  ) throws MalformedURLException, IllegalStateException, IOException {
    sendRedirect(
      status,
      jspContext,
      href,
      params,
      null,
      absolute,
      canonical
    );
  }

  /**
   * Gets the current request URI in context-absolute form.  The contextPath stripped.
   */
  public static String getContextRequestUri(HttpServletRequest request) {
    String requestUri = request.getRequestURI();
    String contextPath = URIEncoder.encodeURI(request.getContextPath());
    int cpLen = contextPath.length();
    if (cpLen > 0) {
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
      if (Arrays.equals(paramTypes, method.getParameterTypes())) {
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
      // if (allow.length() != 0) {
      //   allow.append(", ");
      // }
      allow.append(METHOD_GET);
    }
    if (ALLOW_HEAD) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_HEAD);
    }
    if (ALLOW_POST) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_POST);
    }
    if (ALLOW_PUT) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_PUT);
    }
    if (ALLOW_DELETE) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_DELETE);
    }
    if (ALLOW_TRACE) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_TRACE);
    }
    if (ALLOW_OPTIONS) {
      if (allow.length() != 0) {
        allow.append(", ");
      }
      allow.append(METHOD_OPTIONS);
    }
    response.setHeader("Allow", allow.toString());
  }

  /**
   * Gets the submitted filename for a file upload or {@code null} when unknown.  In addition to the default
   * implementation of {@link Part#getSubmittedFileName()}, also:
   * <ol>
   * <li>Applies a <a href="https://stackoverflow.com/a/2424824">MSIE fix</a></li>
   * <li>Protects against '/' or NULL characters, since servers have POSIX filesystem</li>
   * <li>Returns {@code null} and never empty string (servlet spec is not clear here)</li>
   * </ol>
   * See:
   * <ol>
   * <li><a href="https://stackoverflow.com/a/2424824">How to upload files to server using JSP/Servlet?</a></li>
   * <li><a href="https://www.journaldev.com/2122/servlet-3-file-upload-multipartconfig-part">Servlet 3 File Upload - @MultipartConfig, Part</a></li>
   * </ol>
   *
   * @see  Part#getSubmittedFileName()
   */
  public static String getSubmittedFileName(Part part) {
    if (part != null) {
      String filename = part.getSubmittedFileName();
      if (filename != null && !filename.isEmpty()) {
        filename = Paths.get(filename).getFileName().toString(); // MSIE fix
        if (
          !filename.isEmpty()
          // Servers have POSIX filesystem, make sure absolutely no '/' or NULL characters
          && filename.indexOf('/') == -1
          && filename.indexOf((char)0) == -1
        ) {
          return filename;
        }
      }
    }
    return null;
  }
}
