/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2019  AO Industries, Inc.
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
import com.aoindustries.util.WrappedException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

/**
 * Variant of {@link com.aoindustries.net.URIComponent} that use the
 * {@linkplain ServletRequest#getCharacterEncoding() request encoding} and
 * {@linkplain ServletResponse#getCharacterEncoding() response encoding}.
 * 
 * @see com.aoindustries.net.URIComponent
 *
 * @author  AO Industries, Inc.
 */
public enum URIComponent {

	/**
	 * @see  com.aoindustries.net.URIComponent#BASE
	 */
	BASE {
		@Override
		public com.aoindustries.net.URIComponent getURIComponent() {
			return com.aoindustries.net.URIComponent.BASE;
		}
	},

	/**
	 * @see  com.aoindustries.net.URIComponent#QUERY
	 */
	QUERY {
		@Override
		public com.aoindustries.net.URIComponent getURIComponent() {
			return com.aoindustries.net.URIComponent.QUERY;
		}
	},

	/**
	 * @see  com.aoindustries.net.URIComponent#FRAGMENT
	 */
	FRAGMENT {
		@Override
		public com.aoindustries.net.URIComponent getURIComponent() {
			return com.aoindustries.net.URIComponent.FRAGMENT;
		}
	};

	/**
	 * Gets the underlying implementation of {@link URIComponent}.
	 */
	abstract public com.aoindustries.net.URIComponent getURIComponent();

	/**
	 * @see  com.aoindustries.net.URIComponent#encode(java.lang.String, java.lang.String)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public String encode(String s, ServletResponse response) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			return getURIComponent().encode(s, responseEncoding);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#encode(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public void encode(String s, ServletResponse response, Appendable out) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			getURIComponent().encode(s, responseEncoding, out);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#encode(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public void encode(String s, ServletResponse response, Appendable out, Encoder encoder) throws IOException {
		String responseEncoding = response.getCharacterEncoding();
		try {
			getURIComponent().encode(s, responseEncoding, out, encoder);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#encode(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public void encode(String s, ServletResponse response, StringBuilder sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			getURIComponent().encode(s, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#encode(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  ServletResponse#getCharacterEncoding()
	 */
	public void encode(String s, ServletResponse response, StringBuffer sb) {
		String responseEncoding = response.getCharacterEncoding();
		try {
			getURIComponent().encode(s, responseEncoding, sb);
		} catch(UnsupportedEncodingException e) {
			throw new WrappedException("ServletResponse encoding (" + responseEncoding + ") is expected to always exist", e);
		}
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#decode(java.lang.String, java.lang.String)
	 * @see  ServletUtil#getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public String decode(String s, ServletRequest request) throws UnsupportedEncodingException {
		return getURIComponent().decode(s, ServletUtil.getRequestEncoding(request));
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#decode(java.lang.String, java.lang.String, java.lang.Appendable)
	 * @see  ServletUtil#getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public void decode(String s, ServletRequest request, Appendable out) throws UnsupportedEncodingException, IOException {
		getURIComponent().decode(s, ServletUtil.getRequestEncoding(request), out);
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#decode(java.lang.String, java.lang.String, java.lang.Appendable, com.aoindustries.io.Encoder)
	 * @see  ServletUtil#getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public void decode(String s, ServletRequest request, Appendable out, Encoder encoder) throws UnsupportedEncodingException, IOException {
		getURIComponent().decode(s, ServletUtil.getRequestEncoding(request), out, encoder);
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#decode(java.lang.String, java.lang.String, java.lang.StringBuilder)
	 * @see  ServletUtil#getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public void decode(String s, ServletRequest request, StringBuilder sb) throws UnsupportedEncodingException {
		getURIComponent().decode(s, ServletUtil.getRequestEncoding(request), sb);
	}

	/**
	 * @see  com.aoindustries.net.URIComponent#decode(java.lang.String, java.lang.String, java.lang.StringBuffer)
	 * @see  ServletUtil#getRequestEncoding(javax.servlet.ServletRequest)
	 */
	public void decode(String s, ServletRequest request, StringBuffer sb) throws UnsupportedEncodingException {
		getURIComponent().decode(s, ServletUtil.getRequestEncoding(request), sb);
	}
}
