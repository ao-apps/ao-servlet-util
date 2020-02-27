/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2011, 2016, 2019, 2020  AO Industries, Inc.
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
package com.aoindustries.util.i18n.servlet;

import com.aoindustries.net.URIEncoder;
import com.aoindustries.servlet.http.Cookies;
import com.aoindustries.servlet.http.HttpServletUtil;
import com.aoindustries.util.i18n.EditableResourceBundle;
import java.io.IOException;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Allows any user with the role of translator to edit the translation of the site.
 * This is used in conjunction with the ResourceEditorTag.
 * <p>
 * See <a href="https://aoindustries.com/ao-taglib/apidocs/index.html?com/aoindustries/taglib/ResourceEditorTag.html">ResourceEditorTag</a>
 * </p>
 */
public class EditableResourceServletFilter implements Filter {

	private static final String FILTER_ENABLED_REQUEST_ATTRIBUTE = EditableResourceServletFilter.class.getName()+" . enabled";

	private String role;

	@Override
	public void init(FilterConfig filterConfig) throws ServletException {
		role = filterConfig.getInitParameter("role");
	}

	@Override
	public void doFilter(
		ServletRequest request,
		ServletResponse response,
		FilterChain chain
	) throws IOException, ServletException {
		// Makes sure only one locale filter is applied per request
		if(
			request.getAttribute(FILTER_ENABLED_REQUEST_ATTRIBUTE)==null
			&& (request instanceof HttpServletRequest)
			&& (response instanceof HttpServletResponse)
		) {
			request.setAttribute(FILTER_ENABLED_REQUEST_ATTRIBUTE, Boolean.TRUE);
			try {
				HttpServletRequest httpRequest = (HttpServletRequest)request;
				HttpServletResponse httpResponse = (HttpServletResponse)response;
				if("*".equals(role) || httpRequest.isUserInRole(role)) {
					try {
						// Check for cookie
						boolean modifyAllText = "visible".equals(Cookies.getCookie(httpRequest, EditableResourceBundle.VISIBILITY_COOKIE_NAME));
						// Setup request for editing
						EditableResourceBundle.resetRequest(
							true,
							httpResponse.encodeURL(
								URIEncoder.encodeURI(
									HttpServletUtil.getAbsoluteURL(
										httpRequest,
										"/SetResourceBundleValue"
									)
								)
							),
							modifyAllText
						);
						chain.doFilter(request, response);
					} finally {
						EditableResourceBundle.resetRequest(false, null, false);
					}
				} else {
					// Not allowed to translate
					EditableResourceBundle.resetRequest(false, null, false);
					chain.doFilter(request, response);
				}
			} finally {
				request.removeAttribute(FILTER_ENABLED_REQUEST_ATTRIBUTE);
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}
}
