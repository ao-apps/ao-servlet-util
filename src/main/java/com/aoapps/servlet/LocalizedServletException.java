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
 * along with ao-servlet-util.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoapps.servlet;

import com.aoapps.lang.EmptyArrays;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.i18n.Resources;
import java.io.Serializable;
import javax.servlet.ServletException;

/**
 * Extends <code>ServletException</code> to provide exceptions with user locale error messages.
 *
 * @author  AO Industries, Inc.
 */
public class LocalizedServletException extends ServletException {

	private static final long serialVersionUID = 2L;

	/**
	 * @deprecated  Please use {@link #resources} directly.
	 */
	@Deprecated
	protected final com.aoapps.util.i18n.ApplicationResourcesAccessor accessor;
	protected final Resources resources;
	protected final String key;
	protected final Serializable[] args;

	public LocalizedServletException(Resources resources, String key) {
		super(resources.getMessage(key));
		this.accessor = resources;
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	/**
	 * @deprecated  Please use {@link #LocalizedServletException(com.aoapps.lang.i18n.Resources, java.lang.String)} directly.
	 */
	@Deprecated
	public LocalizedServletException(com.aoapps.util.i18n.ApplicationResourcesAccessor accessor, String key) {
		this((Resources)accessor, key);
	}

	public LocalizedServletException(Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args));
		this.accessor = resources;
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	/**
	 * @deprecated  Please use {@link #LocalizedServletException(com.aoapps.lang.i18n.Resources, java.lang.String, java.io.Serializable...)} directly.
	 */
	@Deprecated
	public LocalizedServletException(com.aoapps.util.i18n.ApplicationResourcesAccessor accessor, String key, Serializable... args) {
		this((Resources)accessor, key, args);
	}

	public LocalizedServletException(Throwable cause, Resources resources, String key) {
		super(resources.getMessage(key), cause);
		this.accessor = resources;
		this.resources = resources;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	/**
	 * @deprecated  Please use {@link #LocalizedServletException(java.lang.Throwable, com.aoapps.lang.i18n.Resources, java.lang.String)} directly.
	 */
	@Deprecated
	public LocalizedServletException(Throwable cause, com.aoapps.util.i18n.ApplicationResourcesAccessor accessor, String key) {
		this(cause, (Resources)accessor, key);
	}

	public LocalizedServletException(Throwable cause, Resources resources, String key, Serializable... args) {
		super(resources.getMessage(key, (Object[])args), cause);
		this.accessor = resources;
		this.resources = resources;
		this.key = key;
		this.args = args;
	}

	/**
	 * @deprecated  Please use {@link #LocalizedServletException(java.lang.Throwable, com.aoapps.lang.i18n.Resources, java.lang.String, java.io.Serializable...)} directly.
	 */
	@Deprecated
	public LocalizedServletException(Throwable cause, com.aoapps.util.i18n.ApplicationResourcesAccessor accessor, String key, Serializable... args) {
		this(cause, (Resources)accessor, key, args);
	}

	@Override
	public String getLocalizedMessage() {
		return resources.getMessage(key, (Object[])args);
	}

	static {
		Throwables.registerSurrogateFactory(LocalizedServletException.class, (template, cause) ->
			new LocalizedServletException(cause, template.resources, template.key, template.args)
		);
	}
}