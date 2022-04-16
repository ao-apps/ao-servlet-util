/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2022  AO Industries, Inc.
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

package com.aoapps.servlet.function;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import javax.servlet.ServletException;

/**
 * A function that is allowed to throw {@link ServletException}, {@link IOException}, and a checked exception.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @see Function
 */
@FunctionalInterface
public interface ServletFunctionE<T, R, Ex extends Throwable> {

	R apply(T t) throws ServletException, IOException, Ex;

	default <V> ServletFunctionE<V, R, Ex> compose(ServletFunctionE<? super V, ? extends T, ? extends Ex> before) throws ServletException, IOException, Ex {
		Objects.requireNonNull(before);
		return v -> apply(before.apply(v));
	}

	default <V> ServletFunctionE<T, V, Ex> andThen(ServletFunctionE<? super R, ? extends V, ? extends Ex> after) throws ServletException, IOException, Ex {
		Objects.requireNonNull(after);
		return t -> after.apply(apply(t));
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	static <T, Ex extends Throwable> ServletFunctionE<T, T, Ex> identity() {
		return t -> t;
	}
}
