/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2016, 2020  AO Industries, Inc.
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
package com.aoindustries.servlet.jsp;

import com.aoindustries.lang.EmptyArrays;
import com.aoindustries.lang.Throwables;
import com.aoindustries.util.i18n.ApplicationResourcesAccessor;
import java.io.Serializable;
import javax.servlet.jsp.JspException;

/**
 * Extends <code>JspException</code> to provide exceptions with user locale error messages.
 *
 * @author  AO Industries, Inc.
 */
public class LocalizedJspException extends JspException {

	private static final long serialVersionUID = 1L;

	protected final ApplicationResourcesAccessor accessor;
	protected final String key;
	protected final Serializable[] args;

	public LocalizedJspException(ApplicationResourcesAccessor accessor, String key) {
		super(accessor.getMessage(key));
		this.accessor = accessor;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public LocalizedJspException(ApplicationResourcesAccessor accessor, String key, Serializable... args) {
		super(accessor.getMessage(key, (Object[])args));
		this.accessor = accessor;
		this.key = key;
		this.args = args;
	}

	public LocalizedJspException(Throwable cause, ApplicationResourcesAccessor accessor, String key) {
		super(accessor.getMessage(key), cause);
		this.accessor = accessor;
		this.key = key;
		this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
	}

	public LocalizedJspException(Throwable cause, ApplicationResourcesAccessor accessor, String key, Serializable... args) {
		super(accessor.getMessage(key, (Object[])args), cause);
		this.accessor = accessor;
		this.key = key;
		this.args = args;
	}

	@Override
	public String getLocalizedMessage() {
		return accessor.getMessage(key, (Object[])args);
	}

	static {
		Throwables.registerSurrogateFactory(LocalizedJspException.class, (template, cause) ->
			new LocalizedJspException(cause, template.accessor, template.key, template.args)
		);
	}
}
