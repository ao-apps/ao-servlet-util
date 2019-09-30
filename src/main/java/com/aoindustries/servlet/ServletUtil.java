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
package com.aoindustries.servlet;

import com.aoindustries.io.Encoder;
import com.aoindustries.net.URIDecoder;
import com.aoindustries.net.URIEncoder;
import com.aoindustries.servlet.http.HttpServletUtil;
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
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
public class ServletUtil {

	private ServletUtil() {
	}

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

	private static final Charset DEFAULT_REQUEST_ENCODING = StandardCharsets.ISO_8859_1;

	/**
	 * Gets the request encoding or ISO-8859-1 when not available.
	 */
	public static String getRequestEncoding(ServletRequest request) {
		String requestEncoding = request.getCharacterEncoding();
		return requestEncoding != null ? requestEncoding : DEFAULT_REQUEST_ENCODING.name();
	}

	/**
	 * @see  URIEncoder#encodeURIComponent(java.lang.String, java.lang.String)
	 * @see  ServletResponse#getCharacterEncoding()
	 *
	 * @deprecated  Please use {@link URIComponent#encode(java.lang.String, javax.servlet.ServletResponse)}
	 */
	@Deprecated
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
	 *
	 * @deprecated  Please use {@link URIComponent#encode(java.lang.String, javax.servlet.ServletResponse, java.lang.Appendable)}
	 */
	@Deprecated
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
	 *
	 * @deprecated  Please use {@link URIComponent#encode(java.lang.String, javax.servlet.ServletResponse, java.lang.Appendable, com.aoindustries.io.Encoder)}
	 */
	@Deprecated
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
	 *
	 * @deprecated  Please use {@link URIComponent#encode(java.lang.String, javax.servlet.ServletResponse, java.lang.StringBuilder)}
	 */
	@Deprecated
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
	 *
	 * @deprecated  Please use {@link URIComponent#encode(java.lang.String, javax.servlet.ServletResponse, java.lang.StringBuffer)}
	 */
	@Deprecated
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
	 *
	 * @deprecated  Please use {@link URIComponent#decode(java.lang.String, javax.servlet.ServletRequest)}
	 */
	@Deprecated
	public static String decodeURIComponent(String uri, ServletRequest request) throws UnsupportedEncodingException {
		return URIDecoder.decodeURIComponent(uri, getRequestEncoding(request));
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 *
	 * @deprecated  Please use {@link URIComponent#decode(java.lang.String, javax.servlet.ServletRequest, java.lang.Appendable)}
	 */
	@Deprecated
	public static void decodeURIComponent(String uri, ServletRequest request, Appendable out) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), out);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 *
	 * @deprecated  Please use {@link URIComponent#decode(java.lang.String, javax.servlet.ServletRequest, java.lang.Appendable, com.aoindustries.io.Encoder)}
	 */
	@Deprecated
	public static void decodeURIComponent(String uri, ServletRequest request, Appendable out, Encoder encoder) throws UnsupportedEncodingException, IOException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), out, encoder);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 *
	 * @deprecated  Please use {@link URIComponent#decode(java.lang.String, javax.servlet.ServletRequest, java.lang.StringBuilder)}
	 */
	@Deprecated
	public static void decodeURIComponent(String uri, ServletRequest request, StringBuilder sb) throws UnsupportedEncodingException {
		URIDecoder.decodeURIComponent(uri, getRequestEncoding(request), sb);
	}

	/**
	 * @see  URIDecoder#decodeURIComponent(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  #getRequestEncoding(javax.servlet.ServletRequest)
	 *
	 * @deprecated  Please use {@link URIComponent#decode(java.lang.String, javax.servlet.ServletRequest, java.lang.StringBuffer)}
	 */
	@Deprecated
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
