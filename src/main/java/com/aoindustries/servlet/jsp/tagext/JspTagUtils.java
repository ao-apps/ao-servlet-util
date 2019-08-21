/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2016  AO Industries, Inc.
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
package com.aoindustries.servlet.jsp.tagext;

import com.aoindustries.servlet.jsp.LocalizedJspException;
import static com.aoindustries.servlet.jsp.tagext.ApplicationResources.accessor;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Static utilities for JSP taglibs.
 *
 * @author  AO Industries, Inc.
 */
public final class JspTagUtils {

	/**
	 * Gets the class name (without package) for the given class.
	 */
	private static String getClassName(Class<?> clazz) {
		String name = clazz.getSimpleName();
		int dotPos = name.lastIndexOf('.');
		return dotPos==-1 ? name : name.substring(dotPos+1);
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @return  the parent tag
	 * @exception  JspException  if parent not found
	 */
	public static <T> T findAncestor(JspTag from, Class<? extends T> clazz) throws JspException {
		T parent = clazz.cast(SimpleTagSupport.findAncestorWithClass(from, clazz));
		if(parent==null) throw new LocalizedJspException(accessor, "JspTagUtils.findAncestor.notFound", getClassName(from.getClass()), getClassName(clazz));
		return parent;
	}

	/**
	 * Make no instances.
	 */
	private JspTagUtils() {
	}
}
