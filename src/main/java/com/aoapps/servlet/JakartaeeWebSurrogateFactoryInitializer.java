/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2020, 2021, 2022, 2025, 2026  AO Industries, Inc.
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

package com.aoapps.servlet;

import com.aoapps.lang.ThrowableSurrogateFactoryInitializer;
import com.aoapps.lang.Throwables;

/**
 * Registers Jakarta EE Web exceptions in {@link Throwables#registerSurrogateFactory(java.lang.Class, com.aoapps.lang.ThrowableSurrogateFactory)}.
 *
 * @author  AO Industries, Inc.
 */
public class JakartaeeWebSurrogateFactoryInitializer implements ThrowableSurrogateFactoryInitializer {

  @Override
  @SuppressWarnings("deprecation")
  public void run() {
    // Note: This matches jakartaee-web-profile-bom:pom.xml

    // From https://jakarta.ee/specifications/platform/10/apidocs/overview-tree

    // jakarta.el:jakarta.el-api:5.0.1
    // Added by ao-encoding-taglib project

    // jakarta.platform:jakarta.jakartaee-web-api:10.0.0
    // Would add a dependency, not doing

    // jakarta.servlet:jakarta.servlet-api:6.0.0
    Throwables.registerSurrogateFactory(jakarta.servlet.ServletException.class, (template, cause) ->
        new jakarta.servlet.ServletException(template.getMessage(), cause)
    );
    Throwables.registerSurrogateFactory(jakarta.servlet.UnavailableException.class, (template, cause) -> {
      jakarta.servlet.UnavailableException newEx = new jakarta.servlet.UnavailableException(template.getMessage(), template.getUnavailableSeconds());
      newEx.initCause(cause);
      return newEx;
    });

    // jakarta.servlet.jsp:jakarta.servlet.jsp-api:3.1.1
    Throwables.registerSurrogateFactory(jakarta.servlet.jsp.JspException.class, (template, cause) ->
        new jakarta.servlet.jsp.JspException(template.getMessage(), cause)
    );
    Throwables.registerSurrogateFactory(jakarta.servlet.jsp.JspTagException.class, (template, cause) ->
        new jakarta.servlet.jsp.JspTagException(template.getMessage(), cause)
    );
    Throwables.registerSurrogateFactory(jakarta.servlet.jsp.SkipPageException.class, (template, cause) ->
        new jakarta.servlet.jsp.SkipPageException(template.getMessage(), cause)
    );
    Throwables.registerSurrogateFactory(jakarta.servlet.jsp.el.ELException.class, (template, cause) ->
        new jakarta.servlet.jsp.el.ELException(template.getMessage(), cause));
    Throwables.registerSurrogateFactory(jakarta.servlet.jsp.el.ELParseException.class, (template, cause) -> {
      jakarta.servlet.jsp.el.ELParseException newEx = new jakarta.servlet.jsp.el.ELParseException(template.getMessage());
      newEx.initCause(cause);
      return newEx;
    });

    // jakarta.servlet.jsp.jstl:jakarta.servlet.jsp.jstl-api:3.0.2
    // Would add a dependency, not doing

    // jakarta.websocket:jakarta.websocket-api:2.1.1
    // Would add a dependency, not doing

    // jakarta.websocket:jakarta.websocket-client-api:2.1.1
    // Would add a dependency, not doing

    // org.glassfish.web:jakarta.servlet.jsp.jstl:3.0.1
    // Would add a dependency, not doing
  }
}
