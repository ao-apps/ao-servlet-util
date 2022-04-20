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

package com.aoapps.servlet.attribute;

import java.util.Collections;
import java.util.Enumeration;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;

/**
 * A specifically resolved context, such as {@link JspContext}, {@link ServletRequest},
 * {@link HttpSession}, and {@link ServletContext}.
 * <p>
 * {@link AttributeEE}: Has scope and context, still needs name.
 * </p>
 *
 * @see  AttributeEE
 * @see  ScopeEE
 *
 * @author  AO Industries, Inc.
 */
public abstract class ContextEE<C> extends com.aoapps.lang.attribute.Context<C> {

  private ContextEE() {
    // Do nothing
  }

  /**
   * Gets the scope for this context.
   */
  @Override
  public abstract ScopeEE<C> getScope();

  /**
   * {@link AttributeEE}: Uses the given name within this scope and context.
   */
  @Override
  public abstract <T> AttributeEE<C, T> attribute(String name);

  /**
   * {@link AttributeEE}: Has {@linkplain JspContext#getAttribute(java.lang.String) page scope and context},
   * still needs name.
   */
  public static final class Page extends ContextEE<JspContext> {

    private final JspContext jspContext;

    Page(JspContext jspContext) {
      this.jspContext = jspContext;
    }

    /**
     * Gets the {@linkplain JspContext#getAttribute(java.lang.String) page scope}.
     */
    @Override
    public ScopeEE.Page getScope() {
      return ScopeEE.PAGE;
    }

    /**
     * {@link AttributeEE}: Uses the given name within this {@linkplain JspContext#getAttribute(java.lang.String) page scope and context}.
     */
    @Override
    public <T> AttributeEE.Page<T> attribute(String name) {
      return new AttributeEE.Page<>(jspContext, name);
    }

    /**
     * {@linkplain JspContext#getAttributeNamesInScope(int) Gets the attribute names}
     * within this {@linkplain JspContext#getAttribute(java.lang.String) page scope and context}.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
      return jspContext.getAttributeNamesInScope(PageContext.PAGE_SCOPE);
    }
  }

  /**
   * {@link AttributeEE}: Uses the given {@linkplain JspContext#getAttribute(java.lang.String) page scope and context},
   * still needs name.
   */
  public static Page page(JspContext jspContext) {
    return new Page(jspContext);
  }

  /**
   * {@link AttributeEE}: Has {@linkplain ServletRequest#getAttribute(java.lang.String) request scope and context},
   * still needs name.
   */
  public static final class Request extends ContextEE<ServletRequest> {

    private final ServletRequest request;

    Request(ServletRequest request) {
      this.request = request;
    }

    /**
     * Gets the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope}.
     */
    @Override
    public ScopeEE.Request getScope() {
      return ScopeEE.REQUEST;
    }

    /**
     * {@link AttributeEE}: Uses the given name within this {@linkplain ServletRequest#getAttribute(java.lang.String) request scope and context}.
     */
    @Override
    public <T> AttributeEE.Request<T> attribute(String name) {
      return new AttributeEE.Request<>(request, name);
    }

    /**
     * {@linkplain ServletRequest#getAttributeNames() Gets the attribute names}
     * within this {@linkplain ServletRequest#getAttribute(java.lang.String) request scope and context}.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
      return request.getAttributeNames();
    }
  }

  /**
   * {@link AttributeEE}: Uses the given {@linkplain ServletRequest#getAttribute(java.lang.String) request scope and context},
   * still needs name.
   */
  public static Request request(ServletRequest request) {
    return new Request(request);
  }

  /**
   * {@link AttributeEE}: Has {@linkplain HttpSession#getAttribute(java.lang.String) session scope and context},
   * still needs name.
   */
  public static final class Session extends ContextEE<HttpSession> {

    private final HttpSession session;

    Session(HttpSession session) {
      this.session = session;
    }

    /**
     * Gets the {@linkplain HttpSession#getAttribute(java.lang.String) session scope}.
     */
    @Override
    public ScopeEE.Session getScope() {
      return ScopeEE.SESSION;
    }

    /**
     * {@link AttributeEE}: Uses the given name within this {@linkplain HttpSession#getAttribute(java.lang.String) session scope and context}.
     */
    @Override
    public <T> AttributeEE.Session<T> attribute(String name) {
      return new AttributeEE.Session<>(session, name);
    }

    /**
     * {@linkplain HttpSession#getAttributeNames() Gets the attribute names}
     * within this {@linkplain HttpSession#getAttribute(java.lang.String) session scope and context}.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
      return (session == null) ? Collections.emptyEnumeration() : session.getAttributeNames();
    }
  }

  /**
   * {@link AttributeEE}: Uses the given {@linkplain HttpSession#getAttribute(java.lang.String) session scope and context},
   * still needs name.
   */
  public static Session session(HttpSession session) {
    return new Session(session);
  }

  /**
   * {@link AttributeEE}: Has {@linkplain ServletContext#getAttribute(java.lang.String) application scope and context},
   * still needs name.
   */
  public static final class Application extends ContextEE<ServletContext> {

    private final ServletContext servletContext;

    Application(ServletContext servletContext) {
      this.servletContext = servletContext;
    }

    /**
     * Gets the {@linkplain ServletContext#getAttribute(java.lang.String) application scope}.
     */
    @Override
    public ScopeEE.Application getScope() {
      return ScopeEE.APPLICATION;
    }

    /**
     * {@link AttributeEE}: Uses the given name within this {@linkplain ServletContext#getAttribute(java.lang.String) application scope and context}.
     */
    @Override
    public <T> AttributeEE.Application<T> attribute(String name) {
      return new AttributeEE.Application<>(servletContext, name);
    }

    /**
     * {@linkplain ServletContext#getAttributeNames() Gets the attribute names}
     * within this {@linkplain ServletContext#getAttribute(java.lang.String) application scope and context}.
     */
    @Override
    public Enumeration<String> getAttributeNames() {
      return servletContext.getAttributeNames();
    }
  }

  /**
   * {@link AttributeEE}: Uses the given {@linkplain ServletContext#getAttribute(java.lang.String) application scope and context},
   * still needs name.
   */
  public static Application application(ServletContext servletContext) {
    return new Application(servletContext);
  }
}
