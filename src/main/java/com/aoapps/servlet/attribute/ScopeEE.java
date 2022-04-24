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

import com.aoapps.lang.LocalizedIllegalArgumentException;
import com.aoapps.lang.i18n.Resources;
import java.io.File;
import java.util.List;
import java.util.ResourceBundle;
import javax.servlet.AsyncContext;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import org.apache.taglibs.standard.tag.common.fmt.RequestEncodingSupport;

/**
 * The most broad concept is scope, including page, request, session, and application.
 * <p>
 * {@link AttributeEE}: Has scope, still needs context and name.
 * </p>
 *
 * @see  AttributeEE
 * @see  ContextEE
 *
 * @author  AO Industries, Inc.
 */
public abstract class ScopeEE<C> extends com.aoapps.lang.attribute.Scope<C> {

  private static final Resources RESOURCES = Resources.getResources(ResourceBundle::getBundle, ScopeEE.class);

  private static final long serialVersionUID = 1L;

  private ScopeEE() {
    // Do nothing
  }

  /**
   * {@link AttributeEE}: Uses the given context within this scope, still needs name.
   */
  @Override
  public abstract ContextEE<C> context(C context);

  /**
   * {@link AttributeEE}: Has scope and name, still needs context.
   */
  public abstract static class Attribute<C, T> extends com.aoapps.lang.attribute.Scope.Attribute<C, T> {

    private static final long serialVersionUID = 1L;

    private Attribute(String name) {
      super(name);
    }

    /**
     * {@link AttributeEE}: Uses the given context within this scope and name.
     */
    @Override
    public abstract AttributeEE<C, T> context(C context);
  }

  /**
   * {@link AttributeEE}: Uses the given name within this scope, still needs context.
   */
  @Override
  public abstract <T> Attribute<C, T> attribute(String name);

  /**
   * {@link AttributeEE}: Has {@linkplain JspContext#getAttribute(java.lang.String) page scope},
   * still needs context and name.
   */
  public static final class Page extends ScopeEE<JspContext> {

    // <editor-fold desc="PageContext">
    /**
     * @see  PageContext#PAGE
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Attribute<Servlet> PAGE = new Attribute<>(PageContext.PAGE);

    /**
     * @see  PageContext#PAGECONTEXT
     */
    public static final Attribute<PageContext> PAGECONTEXT = new Attribute<>(PageContext.PAGECONTEXT);

    /**
     * @see  PageContext#REQUEST
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Attribute<ServletRequest> REQUEST = new Attribute<>(PageContext.REQUEST);

    /**
     * @see  PageContext#RESPONSE
     */
    public static final Attribute<ServletResponse> RESPONSE = new Attribute<>(PageContext.RESPONSE);

    /**
     * @see  PageContext#CONFIG
     */
    public static final Attribute<ServletConfig> CONFIG = new Attribute<>(PageContext.CONFIG);

    /**
     * @see  PageContext#SESSION
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Attribute<HttpSession> SESSION = new Attribute<>(PageContext.SESSION);

    /**
     * @see  PageContext#OUT
     */
    public static final Attribute<JspWriter> OUT = new Attribute<>(PageContext.OUT);

    /**
     * @see  PageContext#APPLICATION
     */
    @SuppressWarnings("FieldNameHidesFieldInSuperclass")
    public static final Attribute<ServletContext> APPLICATION = new Attribute<>(PageContext.APPLICATION);

    /**
     * @see  PageContext#EXCEPTION
     */
    public static final Attribute<Throwable> EXCEPTION = new Attribute<>(PageContext.EXCEPTION);
    // </editor-fold>

    /**
     * The set of allowed scope names.
     */
    public static final String
        SCOPE_PAGE = "page",
        SCOPE_REQUEST = "request",
        SCOPE_SESSION = "session",
        SCOPE_APPLICATION = "application";

    private static final long serialVersionUID = 1L;

    /**
     * Gets the {@link PageContext} scope value for the textual scope name.
     *
     * @return <ul>
     *           <li>{@code null} or {@code ""} or {@link #PAGE}: {@link PageContext#PAGE_SCOPE}</li>
     *           <li>{@link #REQUEST}: {@link PageContext#REQUEST_SCOPE}</li>
     *           <li>{@link #SESSION}: {@link PageContext#SESSION_SCOPE}</li>
     *           <li>{@link #APPLICATION}: {@link PageContext#APPLICATION_SCOPE}</li>
     *         </ul>
     *
     * @throws  LocalizedIllegalArgumentException  if invalid scope
     */
    public static int getScopeId(String scope) throws LocalizedIllegalArgumentException {
      if (scope == null || scope.isEmpty() || SCOPE_PAGE.equals(scope)) {
        return PageContext.PAGE_SCOPE;
      } else if (SCOPE_REQUEST.equals(scope)) {
        return PageContext.REQUEST_SCOPE;
      } else if (SCOPE_SESSION.equals(scope)) {
        return PageContext.SESSION_SCOPE;
      } else if (SCOPE_APPLICATION.equals(scope)) {
        return PageContext.APPLICATION_SCOPE;
      } else {
        throw new LocalizedIllegalArgumentException(RESOURCES, "Page.scope.invalid", scope);
      }
    }

    private Page() {
      // Do nothing
    }

    /**
     * Keep singleton.
     */
    private Object readResolve() {
      return ScopeEE.PAGE;
    }

    /**
     * {@link AttributeEE}: Uses the given context within the {@linkplain JspContext#getAttribute(java.lang.String) page scope},
     * still needs name.
     */
    @Override
    public ContextEE.Page context(JspContext jspContext) {
      return new ContextEE.Page(jspContext);
    }

    /**
     * {@link AttributeEE}: Has {@linkplain JspContext#getAttribute(java.lang.String) page scope} and name,
     * still needs context.
     */
    public static final class Attribute<T> extends ScopeEE.Attribute<JspContext, T> {

      private static final long serialVersionUID = 1L;

      Attribute(String name) {
        super(name);
      }

      /**
       * {@link AttributeEE}: Uses the given context within the {@linkplain JspContext#getAttribute(java.lang.String) page scope} and this name.
       */
      @Override
      public AttributeEE.Page<T> context(JspContext jspContext) {
        return new AttributeEE.Page<>(jspContext, name);
      }
    }

    /**
     * {@link AttributeEE}: Uses the given name within the {@linkplain JspContext#getAttribute(java.lang.String) page scope},
     * still needs context.
     */
    @Override
    public <T> Attribute<T> attribute(String name) {
      return new Attribute<>(name);
    }
  }

  /**
   * {@link AttributeEE}: Uses the {@linkplain JspContext#getAttribute(java.lang.String) page scope},
   * still needs context and name.
   */
  public static final Page PAGE = new Page();

  /**
   * {@link AttributeEE}: Has {@linkplain ServletRequest#getAttribute(java.lang.String) request scope},
   * still needs context and name.
   */
  public static final class Request extends ScopeEE<ServletRequest> {

    // <editor-fold desc="AsyncContext">
    /**
     * @see  AsyncContext#ASYNC_REQUEST_URI
     */
    public static final Attribute<String> ASYNC_REQUEST_URI = new Attribute<>(AsyncContext.ASYNC_REQUEST_URI);

    /**
     * @see  AsyncContext#ASYNC_CONTEXT_PATH
     */
    public static final Attribute<String> ASYNC_CONTEXT_PATH = new Attribute<>(AsyncContext.ASYNC_CONTEXT_PATH);

    /**
     * @see  AsyncContext#ASYNC_PATH_INFO
     */
    public static final Attribute<String> ASYNC_PATH_INFO = new Attribute<>(AsyncContext.ASYNC_PATH_INFO);

    /**
     * @see  AsyncContext#ASYNC_SERVLET_PATH
     */
    public static final Attribute<String> ASYNC_SERVLET_PATH = new Attribute<>(AsyncContext.ASYNC_SERVLET_PATH);

    /**
     * @see  AsyncContext#ASYNC_QUERY_STRING
     */
    public static final Attribute<String> ASYNC_QUERY_STRING = new Attribute<>(AsyncContext.ASYNC_QUERY_STRING);
    // </editor-fold>

    // <editor-fold desc="PageContext">
    /**
     * @see  PageContext#EXCEPTION
     */
    public static final Attribute<Throwable> EXCEPTION = new Attribute<>(PageContext.EXCEPTION);
    // </editor-fold>

    // <editor-fold desc="RequestDispatcher">
    /**
     * @see  RequestDispatcher#FORWARD_REQUEST_URI
     */
    public static final Attribute<String> FORWARD_REQUEST_URI = new Attribute<>(RequestDispatcher.FORWARD_REQUEST_URI);

    /**
     * @see  RequestDispatcher#FORWARD_CONTEXT_PATH
     */
    public static final Attribute<String> FORWARD_CONTEXT_PATH = new Attribute<>(RequestDispatcher.FORWARD_CONTEXT_PATH);

    /**
     * @see  RequestDispatcher#FORWARD_PATH_INFO
     */
    public static final Attribute<String> FORWARD_PATH_INFO = new Attribute<>(RequestDispatcher.FORWARD_PATH_INFO);

    /**
     * @see  RequestDispatcher#FORWARD_SERVLET_PATH
     */
    public static final Attribute<String> FORWARD_SERVLET_PATH = new Attribute<>(RequestDispatcher.FORWARD_SERVLET_PATH);

    /**
     * @see  RequestDispatcher#FORWARD_QUERY_STRING
     */
    public static final Attribute<String> FORWARD_QUERY_STRING = new Attribute<>(RequestDispatcher.FORWARD_QUERY_STRING);

    /**
     * @see  RequestDispatcher#INCLUDE_REQUEST_URI
     */
    public static final Attribute<String> INCLUDE_REQUEST_URI = new Attribute<>(RequestDispatcher.INCLUDE_REQUEST_URI);

    /**
     * @see  RequestDispatcher#INCLUDE_CONTEXT_PATH
     */
    public static final Attribute<String> INCLUDE_CONTEXT_PATH = new Attribute<>(RequestDispatcher.INCLUDE_CONTEXT_PATH);

    /**
     * @see  RequestDispatcher#INCLUDE_PATH_INFO
     */
    public static final Attribute<String> INCLUDE_PATH_INFO = new Attribute<>(RequestDispatcher.INCLUDE_PATH_INFO);

    /**
     * @see  RequestDispatcher#INCLUDE_SERVLET_PATH
     */
    public static final Attribute<String> INCLUDE_SERVLET_PATH = new Attribute<>(RequestDispatcher.INCLUDE_SERVLET_PATH);

    /**
     * @see  RequestDispatcher#INCLUDE_QUERY_STRING
     */
    public static final Attribute<String> INCLUDE_QUERY_STRING = new Attribute<>(RequestDispatcher.INCLUDE_QUERY_STRING);

    /**
     * @see  RequestDispatcher#ERROR_EXCEPTION
     */
    public static final Attribute<Throwable> ERROR_EXCEPTION = new Attribute<>(RequestDispatcher.ERROR_EXCEPTION);

    /**
     * @see  RequestDispatcher#ERROR_EXCEPTION_TYPE
     */
    public static final Attribute<Class<? extends Throwable>> ERROR_EXCEPTION_TYPE = new Attribute<>(RequestDispatcher.ERROR_EXCEPTION_TYPE);

    /**
     * @see  RequestDispatcher#ERROR_MESSAGE
     */
    public static final Attribute<String> ERROR_MESSAGE = new Attribute<>(RequestDispatcher.ERROR_MESSAGE);

    /**
     * @see  RequestDispatcher#ERROR_REQUEST_URI
     */
    public static final Attribute<String> ERROR_REQUEST_URI = new Attribute<>(RequestDispatcher.ERROR_REQUEST_URI);

    /**
     * @see  RequestDispatcher#ERROR_SERVLET_NAME
     */
    public static final Attribute<String> ERROR_SERVLET_NAME = new Attribute<>(RequestDispatcher.ERROR_SERVLET_NAME);

    /**
     * @see  RequestDispatcher#ERROR_STATUS_CODE
     */
    public static final Attribute<Integer> ERROR_STATUS_CODE = new Attribute<>(RequestDispatcher.ERROR_STATUS_CODE);
    // </editor-fold>

    private static final long serialVersionUID = 1L;

    private Request() {
      // Do nothing
    }

    /**
     * Keep singleton.
     */
    private Object readResolve() {
      return ScopeEE.REQUEST;
    }

    /**
     * {@link AttributeEE}: Uses the given context within the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope},
     * still needs name.
     */
    @Override
    public ContextEE.Request context(ServletRequest request) {
      return new ContextEE.Request(request);
    }

    /**
     * {@link AttributeEE}: Has {@linkplain ServletRequest#getAttribute(java.lang.String) request scope} and name,
     * still needs context.
     */
    public static final class Attribute<T> extends ScopeEE.Attribute<ServletRequest, T> {

      private static final long serialVersionUID = 1L;

      Attribute(String name) {
        super(name);
      }

      /**
       * {@link AttributeEE}: Uses the given context within the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope} and this name.
       */
      @Override
      public AttributeEE.Request<T> context(ServletRequest request) {
        return new AttributeEE.Request<>(request, name);
      }
    }

    /**
     * {@link AttributeEE}: Uses the given name within the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope},
     * still needs context.
     */
    @Override
    public <T> Attribute<T> attribute(String name) {
      return new Attribute<>(name);
    }
  }

  /**
   * {@link AttributeEE}: Uses the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope},
   * still needs context and name.
   */
  public static final Request REQUEST = new Request();

  /**
   * {@link AttributeEE}: Has {@linkplain HttpSession#getAttribute(java.lang.String) session scope},
   * still needs context and name.
   */
  public static final class Session extends ScopeEE<HttpSession> {

    // <editor-fold desc="JSTL 1.2">
    /**
     * @see  RequestEncodingSupport#REQUEST_CHAR_SET
     */
    public static final Attribute<String> REQUEST_CHAR_SET = new Attribute<>("javax.servlet.jsp.jstl.fmt.request.charset");

    // </editor-fold>

    private Session() {
      // Do nothing
    }

    private static final long serialVersionUID = 1L;

    /**
     * Keep singleton.
     */
    private Object readResolve() {
      return ScopeEE.SESSION;
    }

    /**
     * {@link AttributeEE}: Uses the given context within the {@linkplain HttpSession#getAttribute(java.lang.String) session scope},
     * still needs name.
     */
    @Override
    public ContextEE.Session context(HttpSession session) {
      return new ContextEE.Session(session);
    }

    /**
     * {@link AttributeEE}: Has {@linkplain HttpSession#getAttribute(java.lang.String) session scope} and name,
     * still needs context.
     */
    public static final class Attribute<T> extends ScopeEE.Attribute<HttpSession, T> {

      private static final long serialVersionUID = 1L;

      Attribute(String name) {
        super(name);
      }

      /**
       * {@link AttributeEE}: Uses the given context within the {@linkplain HttpSession#getAttribute(java.lang.String) session scope} and this name.
       */
      @Override
      public AttributeEE.Session<T> context(HttpSession session) {
        return new AttributeEE.Session<>(session, name);
      }
    }

    /**
     * {@link AttributeEE}: Uses the given name within the {@linkplain HttpSession#getAttribute(java.lang.String) session scope},
     * still needs context.
     */
    @Override
    public <T> Attribute<T> attribute(String name) {
      return new Attribute<>(name);
    }
  }

  /**
   * {@link AttributeEE}: Uses the {@linkplain HttpSession#getAttribute(java.lang.String) session scope},
   * still needs context and name.
   */
  public static final Session SESSION = new Session();

  /**
   * {@link AttributeEE}: Has {@linkplain ServletContext#getAttribute(java.lang.String) application scope},
   * still needs context and name.
   */
  public static final class Application extends ScopeEE<ServletContext> {

    // <editor-fold desc="ServletContext">
    /**
     * @see  ServletContext#ORDERED_LIBS
     */
    public static final Attribute<List<String>> ORDERED_LIBS = new Attribute<>(ServletContext.ORDERED_LIBS);

    /**
     * @see  ServletContext#TEMPDIR
     */
    public static final Attribute<File> TEMPDIR = new Attribute<>(ServletContext.TEMPDIR);
    // </editor-fold>

    private static final long serialVersionUID = 1L;

    private Application() {
      // Do nothing
    }

    /**
     * Keep singleton.
     */
    private Object readResolve() {
      return ScopeEE.APPLICATION;
    }

    /**
     * {@link AttributeEE}: Uses the given context within the {@linkplain ServletContext#getAttribute(java.lang.String) application scope},
     * still needs name.
     */
    @Override
    public ContextEE.Application context(ServletContext servletContext) {
      return new ContextEE.Application(servletContext);
    }

    /**
     * {@link AttributeEE}: Has {@linkplain ServletContext#getAttribute(java.lang.String) application scope} and name,
     * still needs context.
     */
    public static final class Attribute<T> extends ScopeEE.Attribute<ServletContext, T> {

      private static final long serialVersionUID = 1L;

      Attribute(String name) {
        super(name);
      }

      /**
       * {@link AttributeEE}: Uses the given context within the {@linkplain ServletContext#getAttribute(java.lang.String) application scope} and this name.
       */
      @Override
      public AttributeEE.Application<T> context(ServletContext servletContext) {
        return new AttributeEE.Application<>(servletContext, name);
      }
    }

    /**
     * {@link AttributeEE}: Uses the given name within the {@linkplain ServletContext#getAttribute(java.lang.String) application scope},
     * still needs context.
     */
    @Override
    public <T> Attribute<T> attribute(String name) {
      return new Attribute<>(name);
    }
  }

  /**
   * {@link AttributeEE}: Uses the {@linkplain ServletContext#getAttribute(java.lang.String) application scope},
   * still needs context and name.
   */
  public static final Application APPLICATION = new Application();
}
