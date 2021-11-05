/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2016, 2020, 2021  AO Industries, Inc.
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
package com.aoapps.servlet.jsp.tagext;

import com.aoapps.lang.i18n.LocalizedSupplier;
import com.aoapps.lang.i18n.Resources;
import com.aoapps.servlet.jsp.LocalizedJspTagException;
import java.util.Optional;
import java.util.ResourceBundle;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.tagext.JspTag;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Static utilities for JSP taglibs.
 *
 * @author  AO Industries, Inc.
 */
public abstract class JspTagUtils {

	/** Make no instances. */
	private JspTagUtils() {throw new AssertionError();}

	private static final Resources RESOURCES = Resources.getResources(ResourceBundle::getBundle, JspTagUtils.class);

	/**
	 * Generates a tag name based on the class name (without package) for the given class, in the form
	 * {@code <ClassName>}.
	 */
	private static String generateTagName(Class<?> clazz) {
		String name = clazz.getSimpleName();
		int dotPos = name.lastIndexOf('.', name.length() - 2);
		if(dotPos != -1) name = name.substring(dotPos + 1);
		return '<' + name + '>';
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @return  the parent tag when found
	 *
	 * @see  SimpleTagSupport#findAncestorWithClass(javax.servlet.jsp.tagext.JspTag, java.lang.Class)
	 */
	public static <T> Optional<T> findAncestor(JspTag from, Class<? extends T> ancestorClass) {
		return Optional.ofNullable(
			ancestorClass.cast(
				SimpleTagSupport.findAncestorWithClass(from, ancestorClass)
			)
		);
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @param  fromName      The name of the tag searching from, used in generating the exception message,
	 *                       will typically be in the form {@code "<prefix:name>"} or {@code "<name>"}.
	 *
	 * @param  ancestorName  The name of the tag searching for, used in generating the exception message,
	 *                       will typically be in the form {@code "<prefix:name>"} or {@code "<name>"}.
	 *
	 * @return  the parent tag, never {@code null}
	 *
	 * @throws  Ex  if parent not found
	 *
	 * @see  SimpleTagSupport#findAncestorWithClass(javax.servlet.jsp.tagext.JspTag, java.lang.Class)
	 */
	public static <T, Ex extends Throwable> T requireAncestor(LocalizedSupplier<Ex> exceptionSupplier, String fromName, JspTag from, String ancestorName, Class<? extends T> ancestorClass) throws Ex {
		return findAncestor(from, ancestorClass).orElseThrow(
			() -> exceptionSupplier.get(RESOURCES, "findAncestor.notFound", fromName, ancestorName)
		);
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @param  fromName      The name of the tag searching from, used in generating the exception message,
	 *                       will typically be in the form {@code "<prefix:name>"} or {@code "<name>"}.
	 *
	 * @param  ancestorName  The name of the tag searching for, used in generating the exception message,
	 *                       will typically be in the form {@code "<prefix:name>"} or {@code "<name>"}.
	 *
	 * @return  the parent tag, never {@code null}
	 *
	 * @throws  JspTagException  if parent not found
	 *
	 * @see  SimpleTagSupport#findAncestorWithClass(javax.servlet.jsp.tagext.JspTag, java.lang.Class)
	 */
	public static <T> T requireAncestor(String fromName, JspTag from, String ancestorName, Class<? extends T> ancestorClass) throws JspTagException {
		return requireAncestor(LocalizedJspTagException::new, fromName, from, ancestorName, ancestorClass);
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @return  the parent tag, never {@code null}
	 *
	 * @throws  Ex  if parent not found
	 *
	 * @see  SimpleTagSupport#findAncestorWithClass(javax.servlet.jsp.tagext.JspTag, java.lang.Class)
	 *
	 * @deprecated  Please provide tag names to {@link #requireAncestor(java.lang.String, javax.servlet.jsp.tagext.JspTag, java.lang.String, java.lang.Class)}.
	 */
	@Deprecated
	public static <T, Ex extends Throwable> T requireAncestor(LocalizedSupplier<Ex> exceptionSupplier, JspTag from, Class<? extends T> ancestorClass) throws Ex {
		return findAncestor(from, ancestorClass).orElseThrow(
			() -> exceptionSupplier.get(RESOURCES, "findAncestor.notFound", generateTagName(from.getClass()), generateTagName(ancestorClass))
		);
	}

	/**
	 * Finds the first parent tag of the provided class (or subclass) or implementing the provided interface.
	 *
	 * @return  the parent tag, never {@code null}
	 *
	 * @throws  JspTagException  if parent not found
	 *
	 * @see  SimpleTagSupport#findAncestorWithClass(javax.servlet.jsp.tagext.JspTag, java.lang.Class)
	 *
	 * @deprecated  Please provide tag names to {@link #requireAncestor(java.lang.String, javax.servlet.jsp.tagext.JspTag, java.lang.String, java.lang.Class)}.
	 */
	@Deprecated
	public static <T> T requireAncestor(JspTag from, Class<? extends T> ancestorClass) throws JspTagException {
		return requireAncestor(LocalizedJspTagException::new, from, ancestorClass);
	}
}
