/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2020  AO Industries, Inc.
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

import com.aoindustries.lang.ThrowableSurrogateFactoryInitializer;
import com.aoindustries.lang.Throwables;

/**
 * Registers Java EE Web exceptions in {@link Throwables#registerSurrogateFactory(java.lang.Class, com.aoindustries.lang.ThrowableSurrogateFactory)}.
 *
 * @author  AO Industries, Inc.
 */
public class JavaeeWebSetStackTraceInitializer implements ThrowableSurrogateFactoryInitializer {

	@Override
	@SuppressWarnings("deprecation")
	public void run() {
		// From https://docs.oracle.com/javaee/6/api/overview-tree.html
		// JavaEE 7: Review

		// javax:javaee-web-api:6.0
		// Would add a dependency, not doing

		// javax.el:javax.el-api:2.2.5
		// Added by ao-taglib project

		// javax.servlet:javax.servlet-api:3.0.1
		Throwables.registerSurrogateFactory(javax.servlet.ServletException.class, (template, cause) ->
			new javax.servlet.ServletException(template.getMessage(), cause)
		);
		Throwables.registerSurrogateFactory(javax.servlet.UnavailableException.class, (template, cause) -> {
			javax.servlet.UnavailableException newEx = new javax.servlet.UnavailableException(template.getMessage(), template.getUnavailableSeconds());
			newEx.initCause(cause);
			return newEx;
		});

		// javax.servlet.jsp:javax.servlet.jsp-api:2.2.1
		Throwables.registerSurrogateFactory(javax.servlet.jsp.JspException.class, (template, cause) ->
			new javax.servlet.jsp.JspException(template.getMessage(), cause)
		);
		Throwables.registerSurrogateFactory(javax.servlet.jsp.JspTagException.class, (template, cause) ->
			new javax.servlet.jsp.JspTagException(template.getMessage(), cause)
		);
		Throwables.registerSurrogateFactory(javax.servlet.jsp.SkipPageException.class, (template, cause) ->
			new javax.servlet.jsp.SkipPageException(template.getMessage(), cause)
		);
		Throwables.registerSurrogateFactory(javax.servlet.jsp.el.ELException.class, (template, cause) ->
			new javax.servlet.jsp.el.ELException(template.getMessage(), cause));
		Throwables.registerSurrogateFactory(javax.servlet.jsp.el.ELParseException.class, (template, cause) -> {
			javax.servlet.jsp.el.ELParseException newEx = new javax.servlet.jsp.el.ELParseException(template.getMessage());
			newEx.initCause(cause);
			return newEx;
		});

		// javax.servlet:jstl:1.2
		// Would add a dependency, not doing

		// org.glassfish.web:jstl-impl:1.2
		// Would add a dependency, not doing
	}
}
