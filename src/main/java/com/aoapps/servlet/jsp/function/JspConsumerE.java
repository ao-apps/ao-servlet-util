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
import java.util.function.Consumer;
import javax.servlet.jsp.JspException;

/**
 * A consumer that is allowed to throw {@link JspException}, {@link IOException}, and a checked exception.
 *
 * @param  <Ex>  An arbitrary exception type that may be thrown
 *
 * @see Consumer
 */
@FunctionalInterface
public interface JspConsumerE<T, Ex extends Throwable> {

	void accept(T t) throws JspException, IOException, Ex;

	default JspConsumerE<T, Ex> andThen(JspConsumerE<? super T, ? extends Ex> after) throws JspException, IOException, Ex {
		Objects.requireNonNull(after);
		return t -> { accept(t); after.accept(t); };
	}
}
