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

package com.aoapps.servlet;

import com.aoapps.lang.Coercion;
import com.aoapps.lang.CoercionOptimizerInitializer;
import com.aoapps.lang.io.Encoder;
import java.io.Writer;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Registers unwrapping of {@code BodyContentImpl} in {@link Coercion#registerOptimizer(com.aoapps.lang.CoercionOptimizer)}.
 * <p>
 * This is used to access the wrapped write for Catalina's implementation of
 * the servlet BodyContent.  This allows implementations of BufferResult to
 * more efficiently write their contents to recognized writer implementations.
 * This also allows strings to be written maintain their identity for in-context
 * translation tools.
 * </p>
 *
 * @author  AO Industries, Inc.
 */
public class BodyContentImplCoercionOptimizerInitializer implements CoercionOptimizerInitializer {

  private static final Logger logger = Logger.getLogger(BodyContentImplCoercionOptimizerInitializer.class.getName());

  private static final String BODY_CONTENT_IMPL_CLASS = "org.apache.jasper.runtime.BodyContentImpl";
  private static final String WRITER_FIELD = "writer";

  @Override
  public void run() {
    try {
      Class<?> clazz = Class.forName(BODY_CONTENT_IMPL_CLASS);
      // System.err.println("DEBUG: clazz=" + clazz);
      Field field = clazz.getDeclaredField(WRITER_FIELD);
      // System.err.println("DEBUG: field=" + field);
      field.setAccessible(true);
      Coercion.registerOptimizer((Writer out, Encoder encoder) -> {
        Class<? extends Writer> outClass = out.getClass();
        if (outClass == clazz) {
          try {
            Writer writer = (Writer)field.get(out);
            // When the writer field is non-null, BodyContent is pass-through and we may safely directly access the wrapped writer.
            if (writer != null) {
              // Will keep looping to unwrap the wrapped out
              if (logger.isLoggable(Level.FINER)) {
                logger.finer("Successfully unwrapped instance of " + writer.getClass().getName());
              }
              return writer;
            } else {
              // BodyContent is buffering, must use directly
              if (logger.isLoggable(Level.FINER)) {
                logger.finer(clazz.getClass().getName() + " is buffering, nothing to unwrap");
              }
              return out;
            }
          } catch (IllegalAccessException e) {
            throw new AssertionError("The field has already been set accessible", e);
          }
        } else {
          // No unwrapping
          return out;
        }
      });
    } catch (ThreadDeath td) {
      throw td;
    } catch (Error | RuntimeException | ClassNotFoundException | NoSuchFieldException t) {
      if (logger.isLoggable(Level.INFO)) {
        logger.log(
          Level.INFO,
          "Cannot get direct access to the " + BODY_CONTENT_IMPL_CLASS + "." + WRITER_FIELD + " field.  "
          + "Unwrapping of BodyContent disabled.  "
          + "The system will behave correctly, but some optimizations are disabled.",
          t
        );
      }
    }
  }
}
