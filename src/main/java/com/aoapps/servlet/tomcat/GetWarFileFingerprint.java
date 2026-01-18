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
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Gets the WAR file MD5 fingerprint.  This is used to verify the final deployed artifact
 * matches the expected content.
 *
 * <p>We choose MD5 over SHA-256 because Jenkins uses MD5 for fingerprints.  This allows
 * to directly search and find exactly which build deployed the running application.</p>
 *
 * <p>Find the WAR file based on the path the application is deployed to.
 * Uses {@link ServletContext#getRealPath(java.lang.String) ServletContext.getRealPath("/")}
 * then appends {@code ".war"}.  This might be specific to Tomcat installations, and should
 * not be assumed to apply generally.</p>
 */
public class GetWarFileFingerprint extends HttpServlet {

  private static final Logger logger = Logger.getLogger(GetWarFileFingerprint.class.getName());

  private static final long serialVersionUID = 1L;

  /**
   * The file extension for the WAR file.
   */
  private static final String WAR_EXTENSION = ".war";

  /**
   * Computes the MD5 of the given file.
   */
  private static String md5(File file) throws IOException, NoSuchAlgorithmException {
    MessageDigest digest = MessageDigest.getInstance("MD5");
    try (DigestInputStream dis = new DigestInputStream(new FileInputStream(file), digest)) {
      dis.transferTo(OutputStream.nullOutputStream());
    }
    byte[] hash = digest.digest();
    StringBuilder sb = new StringBuilder(hash.length * 2);
    for (byte b : hash) {
      sb.append(String.format("%02x", b));
    }
    return sb.toString();
  }

  /**
   * Computes the MD5 sum of the WAR file, located by using
   * {@link ServletContext#getRealPath(java.lang.String) ServletContext.getRealPath("/")} concatenated with
   * {@link #WAR_EXTENSION}.
   *
   * <p>If unable to find or compute, the specific reason is logged with level {@link Level#WARNING} or
   * {@link Level#SEVERE}, depending on the nature of the failure.  This method is not expected
   * to be used frequently, only after deployments and possibly via occasional audit / monitoring.
   * Logging of these messages is considered best from a security standpoint and are not expected
   * to fill storage space significantly.</p>
   *
   * @return  The non-empty string of the WAR file fingerprint or {@link Optional#empty()} when
   *          unable to compute the fingerprint (in which case the underlying cause is logged).
   */
  @SuppressWarnings({"UseSpecificCatch", "BroadCatchBlock", "TooBroadCatch"})
  public static Optional<String> getWarFileFingerprint(ServletContext context) {
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
      File webappsDir = webRoot.getParentFile();
      if (webappsDir == null) {
        logger.log(Level.WARNING, () -> "The web root does not have a parent: " + webRoot);
        return Optional.empty();
      }
      File warFile = new File(webappsDir, webRoot.getName() + WAR_EXTENSION);
      if (!warFile.exists()) {
        logger.log(Level.WARNING, () -> "The WAR file does not exist: " + warFile);
        return Optional.empty();
      }
      if (!warFile.isFile()) {
        logger.log(Level.WARNING, () -> "The WAR file is not a file: " + warFile);
        return Optional.empty();
      }
      return Optional.of(md5(warFile));
    } catch (Throwable t) {
      logger.log(Level.SEVERE, "Unable to get WAR file fingerprint due to exception", t);
      return Optional.empty();
    }
  }

  private String role;
  private String fingerprint;

  @Override
  public void init(ServletConfig config) throws ServletException {
    super.init(config);
    role = config.getInitParameter("role");
    fingerprint = getWarFileFingerprint(getServletContext()).orElse(null);
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
      if (fingerprint == null) {
        // 500 error
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        try (PrintWriter out = response.getWriter()) {
          out.write("Unable to get WAR file fingerprint.  "
              + "The specific cause has been logged with either WARNING or SEVERE log level.");
        }
      } else {
        // Normal return
        try (PrintWriter out = response.getWriter()) {
          out.append(fingerprint);
        }
      }
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
