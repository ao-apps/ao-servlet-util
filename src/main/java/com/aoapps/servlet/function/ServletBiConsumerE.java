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
import java.util.function.BiConsumer;
import javax.servlet.ServletException;

/**
 * A biconsumer that is allowed to throw {@link ServletException}, {@link IOException}, and a checked exception.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @see BiConsumer
 */
@FunctionalInterface
public interface ServletBiConsumerE<T, U, Ex extends Throwable> {

  void accept(T t, U u) throws ServletException, IOException, Ex;

  default ServletBiConsumerE<T, U, Ex> andThen(ServletBiConsumerE<? super T, ? super U, ? extends Ex> after) throws ServletException, IOException, Ex {
    Objects.requireNonNull(after);

    return (l, r) -> {
      accept(l, r);
      after.accept(l, r);
    };
  }
}
