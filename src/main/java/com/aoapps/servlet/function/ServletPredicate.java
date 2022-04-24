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
import java.util.function.Predicate;
import javax.servlet.ServletException;

/**
 * A predicate that is allowed to throw {@link ServletException} and {@link IOException}.
 *
 * @see Predicate
 */
@FunctionalInterface
public interface ServletPredicate<T> extends ServletPredicateE<T, RuntimeException> {

  @Override
  boolean test(T t) throws ServletException, IOException;

  static <T> ServletPredicate<T> isEqual(Object targetRef) {
    return (null == targetRef)
        ? Objects::isNull
        : targetRef::equals;
  }

  @SuppressWarnings("unchecked")
  static <T> ServletPredicate<T> not(ServletPredicate<? super T> target) throws ServletException, IOException {
    Objects.requireNonNull(target);
    return (ServletPredicate<T>) target.negate();
  }
}
