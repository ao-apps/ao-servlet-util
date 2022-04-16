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

package com.aoapps.servlet.jsp.function;

import java.io.IOException;
import java.util.Objects;
import java.util.function.Predicate;
import javax.servlet.jsp.JspException;

/**
 * A predicate that is allowed to throw {@link JspException}, {@link IOException}, and a checked exception.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @see Predicate
 */
@FunctionalInterface
public interface JspPredicateE<T, Ex extends Throwable> {

	boolean test(T t) throws JspException, IOException, Ex;

	default JspPredicateE<T, Ex> and(JspPredicateE<? super T, ? extends Ex> other) throws JspException, IOException, Ex {
		Objects.requireNonNull(other);
		return t -> test(t) && other.test(t);
	}

	default JspPredicateE<T, Ex> negate() throws JspException, IOException, Ex {
		return t -> !test(t);
	}

	default JspPredicateE<T, Ex> or(JspPredicateE<? super T, ? extends Ex> other) throws JspException, IOException, Ex {
		Objects.requireNonNull(other);
		return t -> test(t) || other.test(t);
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	static <T, Ex extends Throwable> JspPredicateE<T, Ex> isEqual(Object targetRef) {
		return (null == targetRef)
			? Objects::isNull
			: targetRef::equals;
	}

	/**
	 * @param  <Ex>  An arbitrary exception type that may be thrown
	 */
	@SuppressWarnings("unchecked")
	static <T, Ex extends Throwable> JspPredicateE<T, Ex> not(JspPredicateE<? super T, ? extends Ex> target) throws JspException, IOException, Ex {
		Objects.requireNonNull(target);
		return (JspPredicateE<T, Ex>)target.negate();
	}
}
