/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2026  AO Industries, Inc.
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

package com.aoapps.servlet.tomcat;

import com.aoapps.lang.io.ContentType;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Gets the Tomcat parallel deployment version based on the path the application is deployed to.
 * Uses {@link ServletContext#getRealPath(java.lang.String) ServletContext.getRealPath("/")},
 * then takes the version as the final part of the directory name if contains {@code "##"}.
 * This is specific to Tomcat installations, and should not be assumed to apply generally.
 */
public class GetParallelDeploymentVersion extends HttpServlet {

  private static final Logger logger = Logger.getLogger(GetParallelDeploymentVersion.class.getName());

  private static final long serialVersionUID = 1L;

  /**
   * The separator used within the filename before version.
   */
  private static final String SEPARATOR = "##";

  /**
   * Intentionally responding with UNIX newline instead of platform-specific newlines.
   */
  private static final char NEWLINE = '\n';

  /**
   * Gets the parallel deployment version by parsing the deploy directory observed by calling
   * {@link ServletContext#getRealPath(java.lang.String) ServletContext.getRealPath("/")}.
   *
   * <p>If unable to find, the specific reason is logged with level {@link Level#WARNING} or
   * {@link Level#SEVERE}, depending on the nature of the failure.  This method is not expected
   * to be used frequently, only after deployments and possibly via occasional audit / monitoring.
   * Logging of these messages is considered best from a security standpoint and are not expected
   * to fill storage space significantly.</p>
   *
   * @return  The non-empty string of the parallel deployment version or {@link Optional#empty()} when
   *          unable to determine the version (in which case the underlying cause is logged).
   */
  public static Optional<String> getParallelDeploymentVersion(ServletContext context) {
    try {
      String realPath = context.getRealPath("/");
      if (realPath == null) {
        logger.log(Level.WARNING, "ServletContext.getRealPath(\"/\") returned null.  Please ensure Tomcat is configured with <Host unpackWARs=\"true\"> in conf/server.xml.");
        return Optional.empty();
      }
      File webRoot = new File(realPath);
      if (!webRoot.exists()) {
        logger.log(Level.WARNING, () -> "The web root does not exist: " + webRoot);
        return Optional.empty();
      }
      if (!webRoot.isDirectory()) {
        logger.log(Level.WARNING, () -> "The web root is not a directory: " + webRoot);
        return Optional.empty();
      }
      String name = webRoot.getName();
      if (name.isEmpty()) {
        logger.log(Level.WARNING, () -> "Empty web root file name: " + webRoot);
        return Optional.empty();
      }
      int pos = name.indexOf(SEPARATOR);
      if (pos == -1) {
        logger.log(Level.WARNING, () -> "Separator \"" + SEPARATOR + "\" not found in web root file name: " + webRoot);
        return Optional.empty();
      }
      String version = name.substring(pos + SEPARATOR.length());
      if (version.isEmpty()) {
        logger.log(Level.WARNING, () -> "Empty version from web root file name: " + webRoot);
        return Optional.empty();
      }
      // Do not allow carriage returns, newlines or null characters
      if (version.indexOf('\r') != -1) {
        logger.log(Level.WARNING, () -> "Carriage return '\\r' not allowed in web root file name: " + webRoot);
        return Optional.empty();
      }
      if (version.indexOf('\n') != -1) {
        logger.log(Level.WARNING, () -> "Newline '\\n' not allowed in web root file name: " + webRoot);
        return Optional.empty();
      }
      if (version.indexOf('\0') != -1) {
        logger.log(Level.WARNING, () -> "Null character '\\0' not allowed in web root file name: " + webRoot);
        return Optional.empty();
      }
      return Optional.of(version);
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Unable to find parallel deployment version due to exception", t);
      return Optional.empty();
    }
  }

  private String role;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    role = config.getInitParameter("role");
  }

  @Override
  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    // Must have the required role
    if ("*".equals(role) || request.isUserInRole(role)) {
      // text/plain response for both normal return and error message
      response.setContentType(ContentType.TEXT);
      response.setCharacterEncoding(StandardCharsets.UTF_8.name());
      // No caching
      response.setHeader("Cache-Control", "no-store, no-cache, must-revalidate, max-age=0");
      response.setHeader("Pragma", "no-cache");
      response.setDateHeader("Expires", 0);
      // Keep search engines out
      response.setHeader("X-Robots-Tag", "noindex, nofollow");
      Optional<String> version = getParallelDeploymentVersion(getServletContext());
      if (version.isEmpty()) {
        // 500 error
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try (PrintWriter out = response.getWriter()) {
          // Intentionally responding with UNIX newline only
          out.append("Unable to find parallel deployment version.  "
              + "The specific cause has been logged with either WARNING or SEVERE log level." + NEWLINE);
        }
      } else {
        // Normal return
        try (PrintWriter out = response.getWriter()) {
          // Intentionally responding with UNIX newline instead of platform-specific newlines
          out.append(version.get()).append(NEWLINE);
        }
      }
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
