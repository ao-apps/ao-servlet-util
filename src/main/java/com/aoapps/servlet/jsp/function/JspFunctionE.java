/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2022, 2023  AO Industries, Inc.
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

package com.aoapps.servlet.jsp.function;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Function;
import javax.servlet.jsp.JspException;

/**
 * A function that is allowed to throw {@link JspException}, {@link IOException}, and a checked exception.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @see Function
 */
@FunctionalInterface
public interface JspFunctionE<T, R, Ex extends Throwable> {

  R apply(T t) throws JspException, IOException, Ex;

  default <V> JspFunctionE<V, R, Ex> compose(JspFunctionE<? super V, ? extends T, ? extends Ex> before) throws JspException, IOException, Ex {
    Objects.requireNonNull(before);
    return v -> apply(before.apply(v));
  }

  default <V> JspFunctionE<T, V, Ex> andThen(JspFunctionE<? super R, ? extends V, ? extends Ex> after) throws JspException, IOException, Ex {
    Objects.requireNonNull(after);
    return t -> after.apply(apply(t));
  }

  static <T> JspFunctionE<T, T, RuntimeException> identity() {
    return t -> t;
  }
}
