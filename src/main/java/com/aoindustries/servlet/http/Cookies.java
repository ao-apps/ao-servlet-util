/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2009, 2010, 2011, 2016, 2019  AO Industries, Inc.
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
package com.aoindustries.servlet.http;

import com.aoindustries.net.IRI;
import com.aoindustries.net.URIComponent;
import com.aoindustries.net.URIDecoder;
import com.aoindustries.net.URIEncoder;
import java.io.UnsupportedEncodingException;
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
	 * Encodes the name of a cookie in {@link IRI#ENCODING}.
	 *
	 * @return  The encoded name
	 *
	 * @see Cookie#Cookie(java.lang.String, java.lang.String)
	 */
	public static String encodeName(String name) {
		try {
			return URIComponent.QUERY.encode(name, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Encodes the value of a cookie in {@link IRI#ENCODING}.
	 *
	 * @return  The encoded value
	 *
	 * @see Cookie#Cookie(java.lang.String, java.lang.String)
	 */
	public static String encodeValue(String value) {
		try {
			return URIComponent.QUERY.encode(value, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Encodes the comment of a cookie in {@link IRI#ENCODING}.
	 *
	 * @return  The encoded comment
	 *
	 * @see Cookie#setComment(java.lang.String)
	 */
	public static String encodeComment(String comment) {
		try {
			return URIComponent.QUERY.encode(comment, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Encodes the path of a cookie in {@link IRI#ENCODING}.
	 *
	 * @return  The encoded path
	 *
	 * @see Cookie#setPath(java.lang.String)
	 */
	public static String encodePath(String path) {
		try {
			return URIEncoder.encodeURI(path, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Gets the name of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded name
	 *
	 * @see Cookie#getName()
	 */
	public static String decodeName(String name) {
		try {
			return URIComponent.QUERY.decode(name, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Gets the value of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded value
	 *
	 * @see Cookie#getValue()
	 */
	public static String decodeValue(String value) {
		try {
			return URIComponent.QUERY.decode(value, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Gets the comment of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded comment
	 *
	 * @see Cookie#getComment()
	 */
	public static String decodeComment(String comment) {
		try {
			return URIComponent.QUERY.decode(comment, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Gets the path of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded path
	 *
	 * @see Cookie#getPath()
	 */
	public static String decodePath(String path) {
		try {
			return URIDecoder.decodeURI(path, IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	/**
	 * Creates a new cookie, but does not add it to any response.
	 * Encodes name and value in {@link IRI#ENCODING}.
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
	 * Sets the comment of a cookie, encoded in {@link IRI#ENCODING}.
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
	 * Sets the path of a cookie, encoded in {@link IRI#ENCODING}.
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
	 * Gets the name of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded name
	 *
	 * @see Cookie#getName()
	 */
	public static String getName(Cookie cookie) {
		return decodeName(cookie.getName());
	}

	/**
	 * Gets the value of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded value
	 *
	 * @see Cookie#getValue()
	 */
	public static String getValue(Cookie cookie) {
		return decodeValue(cookie.getValue());
	}

	/**
	 * Gets the comment of a cookie, decoded in {@link IRI#ENCODING}.
	 *
	 * @return  The decoded comment
	 *
	 * @see Cookie#getComment()
	 */
	public static String getComment(Cookie cookie) {
		return decodeComment(cookie.getComment());
	}

	/**
	 * Gets the path of a cookie, decoded in {@link IRI#ENCODING}.
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
	 * Encodes name, value, comment, and path in {@link IRI#ENCODING}.
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
	 * Encodes name, value, comment, and path in {@link IRI#ENCODING}.
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
	 * Encodes name in {@link IRI#ENCODING}.
	 * Decodes value in {@link IRI#ENCODING}.
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
	 * Encodes name in {@link IRI#ENCODING}.
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
