/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2015, 2016, 2017, 2020, 2021  AO Industries, Inc.
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
package com.aoapps.servlet;

import com.aoapps.lang.io.LocalizedIOException;
import static com.aoapps.lang.util.PropertiesUtils.RESOURCES;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import javax.servlet.ServletContext;

/**
 * Property utilities for servlet environments.
 * These methods moved here since they were causing a compile-time dependency on servlet APIs
 * for non-servlet related projects.
 */
final public class PropertiesUtils {

	/**
	 * Make no instances.
	 */
	private PropertiesUtils() {}

	/**
	 * Loads properties from a web resource.
	 */
	public static Properties loadFromResource(ServletContext servletContext, String resource) throws IOException {
		Properties props = new Properties();
		InputStream in = servletContext.getResourceAsStream(resource);
		if(in==null) throw new LocalizedIOException(RESOURCES, "PropertiesUtils.readProperties.resourceNotFound", resource);
		try {
			props.load(in);
		} finally {
			in.close();
		}
		return props;
	}
}
