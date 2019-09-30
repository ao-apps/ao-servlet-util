/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2011, 2013, 2016, 2019  AO Industries, Inc.
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

import com.aoindustries.net.IRI;
import com.aoindustries.net.URI;
import com.aoindustries.net.URIParameters;
import com.aoindustries.net.URIParametersUtils;
import com.aoindustries.util.EnumerationIterator;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import javax.servlet.ServletRequest;

/**
 * Gets unmodifiable parameters from the provided request.
 *
 * @author  AO Industries, Inc.
 */
public class ServletRequestParameters implements URIParameters {

	private final ServletRequest request;

	public ServletRequestParameters(ServletRequest request) {
		this.request = request;
	}

	@Override
	public String toString() {
		try {
			return toString(IRI.ENCODING.name());
		} catch(UnsupportedEncodingException e) {
			throw new AssertionError("Standard encoding (" + IRI.ENCODING + ") should always exist", e);
		}
	}

	@Override
	public String toString(String documentEncoding) throws UnsupportedEncodingException {
		return Objects.toString(URIParametersUtils.toQueryString(this, documentEncoding), "");
	}

	@Override
	public String getParameter(String name) {
		return request.getParameter(name);
	}

	@Override
	@SuppressWarnings("unchecked")
	public Iterator<String> getParameterNames() {
		return new EnumerationIterator<>(request.getParameterNames());
	}

	@Override
	public List<String> getParameterValues(String name) {
		String[] values = request.getParameterValues(name);
		return values==null ? null : Collections.unmodifiableList(Arrays.asList(values));
	}

	@Override
	public Map<String, List<String>> getParameterMap() {
		@SuppressWarnings("unchecked") Map<String,String[]> requestMap = request.getParameterMap();
		Map<String,List<String>> map = new LinkedHashMap<>(requestMap.size()*4/3+1);
		for(Map.Entry<String,String[]> entry : requestMap.entrySet()) {
			map.put(
				entry.getKey(),
				Collections.unmodifiableList(
					Arrays.asList(entry.getValue())
				)
			);
		}
		return Collections.unmodifiableMap(map);
	}
}
