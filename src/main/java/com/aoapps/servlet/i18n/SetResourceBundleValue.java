/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2011, 2013, 2016, 2020, 2021, 2022  AO Industries, Inc.
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

package com.aoapps.servlet.i18n;

import com.aoapps.hodgepodge.i18n.EditableResourceBundle;
import com.aoapps.hodgepodge.i18n.ModifiableResourceBundle;
import com.aoapps.lang.i18n.Locales;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Locale;
import java.util.ResourceBundle;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Sets the resource bundle value.  Used by ResourceEditorTag.
 */
public class SetResourceBundleValue extends HttpServlet {

  private static final long serialVersionUID = 1L;

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
      String baseName = request.getParameter("baseName");
      Locale locale = Locales.parseLocale(request.getParameter("locale"));
      String key = request.getParameter("key");
      String value = request.getParameter("value");
      //for (int c=0;c<value.length();c++) System.out.println(Integer.toHexString(value.charAt(c)));
      boolean modified = Boolean.parseBoolean(request.getParameter("modified"));

      // Find the bundle
      ResourceBundle resourceBundle = ResourceBundle.getBundle(baseName, locale);
      if (!resourceBundle.getLocale().equals(locale)) {
        throw new AssertionError("resourceBundle.locale != locale");
      }
      if (!(resourceBundle instanceof ModifiableResourceBundle)) {
        throw new AssertionError("resourceBundle is not a ModifiableResourceBundle");
      }
      if (value.isEmpty()) {
        ((ModifiableResourceBundle)resourceBundle).removeKey(key);
      } else {
        ((ModifiableResourceBundle)resourceBundle).setString(
          key,
          EditableResourceBundle.EMPTY_DISPLAY.equals(value) ? "" : value,
          modified
        );
      }

      // Set request parameters
      PrintWriter out = response.getWriter();
      out.println("<html>");
      out.println("  <head><title>Value Successfully Set</title></head>");
      out.println("  <body>Value Successfully Set</body>");
      out.println("</html>");
    } else {
      response.sendError(HttpServletResponse.SC_FORBIDDEN);
    }
  }
}
