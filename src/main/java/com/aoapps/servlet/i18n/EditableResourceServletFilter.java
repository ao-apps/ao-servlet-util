/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2011, 2016, 2019, 2020, 2021  AO Industries, Inc.
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
import com.aoapps.net.URIEncoder;
import com.aoapps.servlet.attribute.AttributeEE;
import com.aoapps.servlet.attribute.ScopeEE;
import com.aoapps.servlet.http.Cookies;
import com.aoapps.servlet.http.HttpServletUtil;
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
 * See <a href="https://oss.aoapps.com/taglib/apidocs/com.aoapps.taglib/com/aoapps/taglib/ResourceEditorTag.html">ResourceEditorTag</a>
 * </p>
 */
public class EditableResourceServletFilter implements Filter {

	private static final ScopeEE.Request.Attribute<Boolean> FILTER_ENABLED_REQUEST_ATTRIBUTE =
		ScopeEE.REQUEST.attribute(EditableResourceServletFilter.class.getName() + ".enabled");
//	private static final ScopeEE.Attribute<ServletRequest, Boolean> FILTER_ENABLED_REQUEST_ATTRIBUTE =
//		ScopeEE.REQUEST.attribute(EditableResourceServletFilter.class.getName() + ".enabled");
//		AttributeEE.<Boolean>attribute(EditableResourceServletFilter.class.getName() + ".enabled").request();
//		AttributeEE.<Boolean>attribute(EditableResourceServletFilter.class.getName() + ".enabled").scope(ServletRequest.class);
//		AttributeEE.<Boolean>attribute(EditableResourceServletFilter.class.getName() + ".enabled").scope(ScopeEE.REQUEST);

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
		AttributeEE.Request<Boolean> filterEnabledAttribute = FILTER_ENABLED_REQUEST_ATTRIBUTE.context(request);
//		AttributeEE<ServletRequest, Boolean> filterEnabledAttribute = FILTER_ENABLED_REQUEST_ATTRIBUTE.context(request);
		if(
			filterEnabledAttribute.get() == null
			&& (request instanceof HttpServletRequest)
			&& (response instanceof HttpServletResponse)
		) {
			filterEnabledAttribute.set(true);
			try {
				HttpServletRequest httpRequest = (HttpServletRequest)request;
				HttpServletResponse httpResponse = (HttpServletResponse)response;
				if("*".equals(role) || httpRequest.isUserInRole(role)) {
					try {
						// Check for cookie
						boolean modifyAllText = "visible".equals(Cookies.getCookie(httpRequest, EditableResourceBundle.VISIBILITY_COOKIE_NAME));
						// Setup request for editing
						EditableResourceBundle.setThreadSettings(
							new EditableResourceBundle.ThreadSettings(
								httpResponse.encodeURL(
									URIEncoder.encodeURI(
										HttpServletUtil.getAbsoluteURL(
											httpRequest,
											"/SetResourceBundleValue"
										)
									)
								),
								EditableResourceBundle.ThreadSettings.Mode.MARKUP,
								modifyAllText
							)
						);
						chain.doFilter(request, response);
					} finally {
						EditableResourceBundle.resetThreadSettings();
					}
				} else {
					// Not allowed to translate
					EditableResourceBundle.resetThreadSettings();
					chain.doFilter(request, response);
				}
			} finally {
				filterEnabledAttribute.remove();
			}
		} else {
			chain.doFilter(request, response);
		}
	}

	@Override
	public void destroy() {
	}
}
