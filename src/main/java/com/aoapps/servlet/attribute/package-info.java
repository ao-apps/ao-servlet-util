/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2021, 2022  AO Industries, Inc.
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

/**
 * Utilities for working with various attribute scopes.
 * <p>
 * This API has four concepts:
 * </p>
 * <ol>
 *   <li>{@link ScopeEE} - The most broad concept is scope, including page, request, session, and application.
 *                         Does not yet have a resolved context or attribute name.</li>
 *   <li>{@link ContextEE} - A specifically resolved context, such as {@link javax.servlet.jsp.JspContext},
 *                           {@link javax.servlet.ServletRequest}, {@link javax.servlet.http.HttpSession}, and
 *                           {@link javax.servlet.ServletContext}.
 *                           Does not yet have an attribute name.</li>
 *   <li>{@link AttributeEE} - An attribute has both context and name and is used for value access.</li>
 *   <li>{@link AttributeEE.Name} - A name without any specific scope or context.</li>
 * </ol>
 * <p>
 * Ultimately, the goal is to get to an attribute, which means having both a fully resolved context and a name.  The
 * API supports arriving at an attribute in any order, such as <code>scope → context → name</code> or
 * <code>name → context</code>.
 * </p>
 * <p>
 * There is also a set of static utility methods, to which the rest of the API ends up calling.
 * For one-off attribute access, these static methods may be more succinct.
 * </p>
 *
 * @author  AO Industries, Inc.
 */

package com.aoapps.servlet.attribute;
