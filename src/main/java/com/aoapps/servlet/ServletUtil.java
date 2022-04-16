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

package com.aoapps.servlet;

import com.aoapps.servlet.http.HttpServletUtil;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.jsp.SkipPageException;

/**
 * Static utilities that may be useful by servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 *
 * @see HttpServletUtil
 */
public final class ServletUtil {

	/** Make no instances. */
	private ServletUtil() {throw new AssertionError();}

	/**
	 * @see #SKIP_PAGE_EXCEPTION
	 */
	private static final class SingletonSkipPageException extends SkipPageException {

		private static final long serialVersionUID = 1L;

		private SingletonSkipPageException() {
			super(
				"The calling page must cease evaluation." + System.lineSeparator()
				+ "Please note, this is a shared exception instance for efficiency." + System.lineSeparator()
				+ "The stack trace has been reduced to the top-most stack element." + System.lineSeparator()
				+ "Code that catches SkipPageException should be prepared to provide stack traces." + System.lineSeparator()
				+ "Code that catches JspException might also benefit from handling SkipPageException."
			);
			// Hides any stack trace from original caller that first instantiated the object.
			StackTraceElement[] stackTrace = getStackTrace();
			if(stackTrace != null && stackTrace.length > 1) {
				setStackTrace(
					new StackTraceElement[] {
						stackTrace[0]
					}
				);
			}
		}

		private Object readResolve() {
			return SKIP_PAGE_EXCEPTION;
		}
	}

	/**
	 * A shared {@link SkipPageException} instance to avoid exception creation overhead
	 * for the routine operation of skipping pages.
	 */
	public static final SkipPageException SKIP_PAGE_EXCEPTION = new SingletonSkipPageException();

	private static final Charset DEFAULT_REQUEST_ENCODING = StandardCharsets.ISO_8859_1;

	/**
	 * Gets the request encoding or ISO-8859-1 when not available.
	 */
	public static String getRequestEncoding(ServletRequest request) {
		String requestEncoding = request.getCharacterEncoding();
		return requestEncoding != null ? requestEncoding : DEFAULT_REQUEST_ENCODING.name();
	}

	/**
	 * Sets the response content type.  Unlike {@link ServletResponse#setContentType(java.lang.String)},
	 * throws an exception when unable to set the content type.
	 *
	 * @throws ServletException when unable to change the content type
	 */
	public static void setContentType(ServletResponse response, String contentType, String characterEncoding) throws ServletException {
		response.setContentType(contentType);
		response.setCharacterEncoding(characterEncoding);
		String actualContentType = response.getContentType();
		if(actualContentType != null) {
			int semiPos = actualContentType.indexOf(';');
			if(semiPos != -1) actualContentType = actualContentType.substring(0, semiPos);
		}
		if(!contentType.equals(actualContentType)) throw new ServletException("Unable to set content type, response already committed? expectedContentType = " + contentType + ", actualContentType = " + actualContentType);
	}

	/**
	 * Sets the response content type.  Unlike {@link ServletResponse#setContentType(java.lang.String)},
	 * throws an exception when unable to set the content type.
	 *
	 * @throws ServletException when unable to change the content type
	 */
	public static void setContentType(ServletResponse response, String contentType, Charset characterEncoding) throws ServletException {
		setContentType(response, contentType, characterEncoding.name());
	}
}
