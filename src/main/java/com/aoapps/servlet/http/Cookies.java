/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2009, 2010, 2011, 2016, 2019, 2021  AO Industries, Inc.
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
package com.aoapps.servlet.http;

import com.aoapps.net.URIDecoder;
import com.aoapps.net.URIEncoder;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Helper utility to set, get, remove, encode, and decode cookies.
 */
public final class Cookies {

	private Cookies() {
	}

	/**
	 * Encodes the name of a cookie via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 *
	 * @return  The encoded name
	 *
	 * @see Cookie#Cookie(java.lang.String, java.lang.String)
	 */
	public static String encodeName(String name) {
		return URIEncoder.encodeURIComponent(name);
	}

	/**
	 * Encodes the value of a cookie via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 *
	 * @return  The encoded value
	 *
	 * @see Cookie#Cookie(java.lang.String, java.lang.String)
	 */
	public static String encodeValue(String value) {
		return URIEncoder.encodeURIComponent(value);
	}

	/**
	 * Encodes the comment of a cookie via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 *
	 * @return  The encoded comment
	 *
	 * @see Cookie#setComment(java.lang.String)
	 */
	public static String encodeComment(String comment) {
		return URIEncoder.encodeURIComponent(comment);
	}

	/**
	 * Encodes the path of a cookie via {@link URIEncoder#encodeURI(java.lang.String)}.
	 *
	 * @return  The encoded path
	 *
	 * @see Cookie#setPath(java.lang.String)
	 */
	public static String encodePath(String path) {
		return URIEncoder.encodeURI(path);
	}

	/**
	 * Gets the name of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded name
	 *
	 * @see Cookie#getName()
	 */
	public static String decodeName(String name) {
		return URIDecoder.decodeURIComponent(name);
	}

	/**
	 * Gets the value of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded value
	 *
	 * @see Cookie#getValue()
	 */
	public static String decodeValue(String value) {
		return URIDecoder.decodeURIComponent(value);
	}

	/**
	 * Gets the comment of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded comment
	 *
	 * @see Cookie#getComment()
	 */
	public static String decodeComment(String comment) {
		return URIDecoder.decodeURIComponent(comment);
	}

	/**
	 * Gets the path of a cookie, decoded via {@link URIDecoder#decodeURI(java.lang.String)}.
	 *
	 * @return  The decoded path
	 *
	 * @see Cookie#getPath()
	 */
	public static String decodePath(String path) {
		return URIDecoder.decodeURI(path);
	}

	/**
	 * Creates a new cookie, but does not add it to any response.
	 * Encodes name and value via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 *
	 * @return  The new cookie
	 *
	 * @see Cookie#Cookie(java.lang.String, java.lang.String)
	 */
	public static Cookie newCookie(String name, String value) {
		return new Cookie(
			encodeName(name),
			encodeValue(value)
		);
	}

	/**
	 * Sets the comment of a cookie, encoded via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 *
	 * @return  The encoded comment
	 *
	 * @see Cookie#setComment(java.lang.String)
	 */
	public static String setComment(Cookie cookie, String comment) {
		String encodedComment = encodeComment(comment);
		cookie.setComment(encodedComment);
		return encodedComment;
	}

	/**
	 * Sets the path of a cookie, encoded via {@link URIEncoder#encodeURI(java.lang.String)}.
	 *
	 * @return  The encoded path
	 *
	 * @see Cookie#setPath(java.lang.String)
	 */
	public static String setPath(Cookie cookie, String path) {
		String encodedPath = encodePath(path);
		cookie.setPath(encodedPath);
		return encodedPath;
	}

	/**
	 * Gets the name of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded name
	 *
	 * @see Cookie#getName()
	 */
	public static String getName(Cookie cookie) {
		return decodeName(cookie.getName());
	}

	/**
	 * Gets the value of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded value
	 *
	 * @see Cookie#getValue()
	 */
	public static String getValue(Cookie cookie) {
		return decodeValue(cookie.getValue());
	}

	/**
	 * Gets the comment of a cookie, decoded via {@link URIDecoder#decodeURIComponent(java.lang.String)}.
	 *
	 * @return  The decoded comment
	 *
	 * @see Cookie#getComment()
	 */
	public static String getComment(Cookie cookie) {
		return decodeComment(cookie.getComment());
	}

	/**
	 * Gets the path of a cookie, decoded via {@link URIDecoder#decodeURI(java.lang.String)}.
	 *
	 * @return  The decoded path
	 *
	 * @see Cookie#getPath()
	 */
	public static String getPath(Cookie cookie) {
		return decodePath(cookie.getPath());
	}

	/**
	 * Creates a new cookie, but does not add it to any response.
	 * Encodes name, value, and comment via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 * Encodes path via {@link URIEncoder#encodeURI(java.lang.String)}.
	 */
	public static Cookie newCookie(
		HttpServletRequest request,
		String name,
		String value,
		String comment,
		int maxAge,
		boolean secure,
		boolean contextOnlyPath
	) {
		Cookie newCookie = newCookie(name, value);
		setComment(newCookie, comment);
		newCookie.setMaxAge(maxAge);
		newCookie.setSecure(secure && request.isSecure());
		String path;
		if(contextOnlyPath) {
			path = request.getContextPath() + "/";
			//if(path.length()==0) path = "/";
		} else {
			path = "/";
		}
		setPath(newCookie, path);
		return newCookie;
	}

	/**
	 * Adds a new cookie to the response.
	 * Encodes name, value, and comment via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 * Encodes path via {@link URIEncoder#encodeURI(java.lang.String)}.
	 */
	public static void addCookie(
		HttpServletRequest request,
		HttpServletResponse response,
		String name,
		String value,
		String comment,
		int maxAge,
		boolean secure,
		boolean contextOnlyPath
	) {
		response.addCookie(newCookie(request, name, value, comment, maxAge, secure, contextOnlyPath));
	}

	/**
	 * Gets a cookie value given its name or <code>null</code> if not found.
	 * Encodes name and value via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 */
	public static String getCookie(HttpServletRequest request, String name) {
		Cookie[] cookies = request.getCookies();
		if(cookies != null) {
			String encodedName = encodeName(name);
			for(Cookie cookie : cookies) {
				if(cookie.getName().equals(encodedName)) return getValue(cookie);
			}
		}
		return null;
	}

	/**
	 * Removes a cookie by adding it with maxAge of zero.
	 * Encodes name via {@link URIEncoder#encodeURIComponent(java.lang.String)}.
	 */
	public static void removeCookie(
		HttpServletRequest request,
		HttpServletResponse response,
		String name,
		boolean secure,
		boolean contextOnlyPath
	) {
		addCookie(request, response, name, "Removed", null, 0, secure, contextOnlyPath);
	}
}
