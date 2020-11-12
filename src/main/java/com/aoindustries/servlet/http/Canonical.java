/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2019, 2020  AO Industries, Inc.
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

import javax.servlet.http.HttpServletResponse;

/**
 * Coordinates when canonical URLs should be generated during {@linkplain HttpServletResponse#encodeURL(java.lang.String) response URL rewriting} (such as
 * information added when cookies are disabled).  These URLs without per-user settings are used in things like
 * <a href="https://support.google.com/webmasters/answer/139066">Canonical URLs</a>,
 * <a href="https://schema.org/BreadcrumbList">BreadcrumbList</a>,
 * and <a href="https://www.sitemaps.org/">Sitemaps</a>.
 * <p>
 * This is implemented as a {@linkplain ThreadLocal thread local}, so the state must be set by the thread
 * that will invoke {@link HttpServletResponse#encodeURL(java.lang.String)} (and related methods).
 * Thus, it would be inappropriate to set this in a broad, asynchronous scope where the thread
 * handling the request may change.
 * </p>
 */
abstract public class Canonical implements AutoCloseable {

	private Canonical() {
	}

	/**
	 * Restores the previous state of the canonical {@link ThreadLocal}.
	 */
	@Override
	public abstract void close();

	private static final ThreadLocal<Boolean> canonicalUrls = new ThreadLocal<Boolean>() {
		@Override
		protected Boolean initialValue() {
			return Boolean.FALSE;
		}
	};

	/**
	 * Gets the current state of the canonical {@link ThreadLocal}.
	 */
	public static boolean get() {
		return canonicalUrls.get();
	}

	private static final Canonical restoreFalse = new Canonical() {
		@Override
		public void close() {
			canonicalUrls.set(Boolean.FALSE);
		}
	};

	private static final Canonical restoreTrue = new Canonical() {
		@Override
		public void close() {
			canonicalUrls.set(Boolean.TRUE);
		}
	};

	/**
	 * Sets the current state of the canonical {@link ThreadLocal} to the given value.
	 * This should be used in a try-with-resources block to ensure the previous
	 * value is restored.
	 */
	public static Canonical set(boolean value) {
		boolean previous = canonicalUrls.get();
		if(previous != value) canonicalUrls.set(value);
		return previous ? restoreTrue : restoreFalse;
	}

	/**
	 * Sets the current state of the canonical {@link ThreadLocal} to {@code true}.
	 * This should be used in a try-with-resources block to ensure the previous
	 * value is restored.
	 */
	public static Canonical set() {
		return set(true);
	}

	/**
	 * Sets the current state of the canonical {@link ThreadLocal} to {@code false}.
	 * This should be used in a try-with-resources block to ensure the previous
	 * value is restored.
	 */
	public static Canonical clear() {
		return set(false);
	}

	/**
	 * Sets the state of the canonical {@link ThreadLocal} and invokes
	 * {@link HttpServletResponse#encodeURL(java.lang.String)}.
	 */
	@SuppressWarnings("try")
	public static String encodeCanonicalURL(HttpServletResponse response, String url) {
		try (Canonical c = set()) {
			return response.encodeURL(url);
		}
	}

	/**
	 * Sets the state of the canonical {@link ThreadLocal} and invokes
	 * {@link HttpServletResponse#encodeRedirectURL(java.lang.String)}.
	 */
	@SuppressWarnings("try")
	public static String encodeCanonicalRedirectURL(HttpServletResponse response, String url) {
		try (Canonical c = set()) {
			return response.encodeRedirectURL(url);
		}
	}
}
