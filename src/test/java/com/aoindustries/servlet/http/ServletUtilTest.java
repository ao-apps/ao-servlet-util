/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2008, 2009, 2010, 2011, 2016  AO Industries, Inc.
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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * @author  AO Industries, Inc.
 */
public class ServletUtilTest extends TestCase {

	public ServletUtilTest(String testName) {
		super(testName);
	}

	@Override
	protected void setUp() throws Exception {
	}

	@Override
	protected void tearDown() throws Exception {
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ServletUtilTest.class);
		return suite;
	}

	public void testGetAbsolutePath() throws Exception {
		assertEquals("/test/", ServletUtil.getAbsolutePath("/test/page.jsp", "./"));
		assertEquals("/test/other.jsp", ServletUtil.getAbsolutePath("/test/subdir/page.jsp", "/test/other.jsp"));
		assertEquals("/test/other.jsp", ServletUtil.getAbsolutePath("/test/subdir/page.jsp", "../other.jsp"));
		assertEquals("/test/other.jsp", ServletUtil.getAbsolutePath("/test/subdir/page.jsp", "./.././other.jsp"));
		assertEquals("/test/subdir/other.jsp", ServletUtil.getAbsolutePath("/test/page.jsp", "subdir/other.jsp"));
		assertEquals("/test/other.jsp", ServletUtil.getAbsolutePath("/test/page.jsp", "other.jsp"));
		assertEquals("/other.jsp", ServletUtil.getAbsolutePath("/page.jsp", "other.jsp"));
	}
}
