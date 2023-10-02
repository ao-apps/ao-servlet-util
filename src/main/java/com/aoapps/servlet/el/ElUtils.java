/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2023  AO Industries, Inc.
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

package com.aoapps.servlet.el;

import javax.el.ELContext;
import javax.el.ValueExpression;

/**
 * Static utilities for Expression Language.
 *
 * @author  AO Industries, Inc.
 */
public final class ElUtils {

  /** Make no instances. */
  private ElUtils() {
    throw new AssertionError();
  }

  /**
   * Evaluates an expression then casts to the provided type.
   */
  public static <T> T resolveValue(ValueExpression expression, Class<T> type, ELContext elContext) {
    if (expression == null) {
      return null;
    } else {
      return type.cast(expression.getValue(elContext));
    }
  }

  /**
   * Casts or evaluates an expression then casts to the provided type.
   */
  public static <T> T resolveValue(Object value, Class<T> type, ELContext elContext) {
    if (value == null) {
      return null;
    } else if (value instanceof ValueExpression) {
      return resolveValue((ValueExpression) value, type, elContext);
    } else {
      return type.cast(value);
    }
  }
}
