/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2016, 2020, 2021, 2022  AO Industries, Inc.
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

package com.aoapps.servlet.jsp;

import com.aoapps.lang.EmptyArrays;
import com.aoapps.lang.Throwables;
import com.aoapps.lang.exception.LocalizedException;
import com.aoapps.lang.i18n.Resources;
import java.io.Serializable;
import javax.servlet.jsp.JspTagException;

/**
 * Extends <code>JspTagException</code> to provide exceptions with user locale error messages.
 *
 * @author  AO Industries, Inc.
 */
public class LocalizedJspTagException extends JspTagException implements LocalizedException {

  private static final long serialVersionUID = 3L;

  protected final Resources resources;
  protected final String key;
  protected final Serializable[] args;

  public LocalizedJspTagException(Resources resources, String key) {
    super(resources.getMessage(key));
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public LocalizedJspTagException(Resources resources, String key, Serializable... args) {
    super(resources.getMessage(key, (Object[]) args));
    this.resources = resources;
    this.key = key;
    this.args = args;
  }

  public LocalizedJspTagException(Throwable cause, Resources resources, String key) {
    super(resources.getMessage(key), cause);
    this.resources = resources;
    this.key = key;
    this.args = EmptyArrays.EMPTY_SERIALIZABLE_ARRAY;
  }

  public LocalizedJspTagException(Throwable cause, Resources resources, String key, Serializable... args) {
    super(resources.getMessage(key, (Object[]) args), cause);
    this.resources = resources;
    this.key = key;
    this.args = args;
  }

  @Override
  public String getLocalizedMessage() {
    return resources.getMessage(key, (Object[]) args);
  }

  @Override
  public final Resources getResources() {
    return resources;
  }

  @Override
  public final String getKey() {
    return key;
  }

  /**
   * @return  No defensive copy
   */
  @Override
  @SuppressWarnings("ReturnOfCollectionOrArrayField")
  public final Serializable[] getArgs() {
    return args;
  }

  static {
    Throwables.registerSurrogateFactory(LocalizedJspTagException.class, (template, cause) ->
        new LocalizedJspTagException(cause, template.resources, template.key, template.args)
    );
  }
}
