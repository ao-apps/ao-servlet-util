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

import com.aoapps.lang.attribute.Attribute;
import com.aoapps.lang.attribute.Scope;
import com.aoapps.lang.function.BiFunctionE;
import com.aoapps.lang.function.FunctionE;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.TimeZone;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.jstl.core.Config;
import javax.servlet.jsp.jstl.fmt.LocalizationContext;
import javax.sql.DataSource;

/**
 * An attribute has scope, context, and name and is used for value access.
 *
 * @see  ContextEE
 * @see  ScopeEE
 *
 * @author  AO Industries, Inc.
 */
public abstract class AttributeEE<C, T> extends com.aoapps.lang.attribute.Attribute<C, T> {

  private AttributeEE(String name) {
    super(name);
  }

  /**
   * Gets the context for this attribute.
   */
  @Override
  public abstract ContextEE<C> getContext();

  /**
   * A {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
   */
  public static final class Page<T> extends AttributeEE<JspContext, T> {

    private final JspContext jspContext;

    Page(JspContext jspContext, String name) {
      super(name);
      this.jspContext = jspContext;
    }

    /**
     * Gets the {@linkplain JspContext#getAttribute(java.lang.String) page context} for this attribute.
     */
    @Override
    public ContextEE.Page getContext() {
      return new ContextEE.Page(jspContext);
    }

    /**
     * Initializes this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute},
     * returning a backup value, which must be {@link OldValue#close() closed} to restore the old value.
     * This is best used in try-with-resources.
     */
    @Override
    public OldValue init(T value) {
      Object oldValue = jspContext.getAttribute(name);
      if (value != oldValue) {
        jspContext.setAttribute(name, value);
      }
      return new OldValue(oldValue) {
        @Override
        public void close() {
          jspContext.setAttribute(name, oldValue);
        }
      };
    }

    /**
     * Initializes this {@linkplain JspContext#getAttribute(java.lang.String, int) page-scope attribute},
     * returning a backup value, which must be {@link OldValue#close() closed} to restore the old value.
     * This is best used in try-with-resources.
     */
    public OldValue init(int scope, T value) {
      Object oldValue = jspContext.getAttribute(name, scope);
      if (value != oldValue) {
        jspContext.setAttribute(name, value, scope);
      }
      return new OldValue(oldValue) {
        @Override
        public void close() {
          jspContext.setAttribute(name, oldValue, scope);
        }
      };
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T compute(
        JspContext jspContext,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name);
        T newValue = remappingFunction.apply(name, oldValue);
        if (newValue != oldValue) {
          jspContext.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T compute(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return compute(jspContext, name, remappingFunction);
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T compute(
        JspContext jspContext,
        int scope,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name, scope);
        T newValue = remappingFunction.apply(name, oldValue);
        if (newValue != oldValue) {
          jspContext.setAttribute(name, newValue, scope);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public <Ex extends Throwable> T compute(
        int scope,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return compute(jspContext, scope, name, remappingFunction);
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public static <T, Ex extends Throwable> T computeIfAbsent(
        JspContext jspContext,
        String name,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T value = (T) jspContext.getAttribute(name);
        if (value == null) {
          value = mappingFunction.apply(name);
          if (value != null) {
            jspContext.setAttribute(name, value);
          }
        }
        return value;
      }
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    @Override
    public <Ex extends Throwable> T computeIfAbsent(
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      return computeIfAbsent(jspContext, name, mappingFunction);
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public static <T, Ex extends Throwable> T computeIfAbsent(
        JspContext jspContext,
        int scope,
        String name,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T value = (T) jspContext.getAttribute(name, scope);
        if (value == null) {
          value = mappingFunction.apply(name);
          if (value != null) {
            jspContext.setAttribute(name, value, scope);
          }
        }
        return value;
      }
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public <Ex extends Throwable> T computeIfAbsent(
        int scope,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      return computeIfAbsent(jspContext, scope, name, mappingFunction);
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T computeIfPresent(
        JspContext jspContext,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name);
        if (oldValue != null) {
          T newValue = remappingFunction.apply(name, oldValue);
          if (newValue != oldValue) {
            jspContext.setAttribute(name, newValue);
          }
          return newValue;
        } else {
          return null;
        }
      }
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T computeIfPresent(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return computeIfPresent(jspContext, name, remappingFunction);
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T computeIfPresent(
        JspContext jspContext,
        int scope,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name, scope);
        if (oldValue != null) {
          T newValue = remappingFunction.apply(name, oldValue);
          if (newValue != oldValue) {
            jspContext.setAttribute(name, newValue, scope);
          }
          return newValue;
        } else {
          return null;
        }
      }
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public <Ex extends Throwable> T computeIfPresent(
        int scope,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return computeIfPresent(jspContext, scope, name, remappingFunction);
    }

    /**
     * {@linkplain JspContext#findAttribute(java.lang.String) Finds an attribute} in page,
     * request, session (if valid), then application scopes.
     *
     * @see  JspContext#findAttribute(java.lang.String)
     */
    public static <T> T find(JspContext jspContext, String name) {
      @SuppressWarnings("unchecked")
      T value = (T) jspContext.findAttribute(name);
      return value;
    }

    /**
     * {@linkplain JspContext#findAttribute(java.lang.String) Finds this attribute} in page,
     * request, session (if valid), then application scopes.
     *
     * @see  JspContext#findAttribute(java.lang.String)
     */
    public T find() {
      return find(jspContext, name);
    }

    /**
     * {@linkplain JspContext#getAttributesScope(java.lang.String) Finds an attribute's scope} in page,
     * request, session (if valid), then application scopes.
     *
     * @see  JspContext#getAttributesScope(java.lang.String)
     */
    public static int findScope(JspContext jspContext, String name) {
      return jspContext.getAttributesScope(name);
    }

    /**
     * {@linkplain JspContext#getAttributesScope(java.lang.String) Finds this attribute's scope} in page,
     * request, session (if valid), then application scopes.
     *
     * @see  JspContext#getAttributesScope(java.lang.String)
     */
    public int findScope() {
      return findScope(jspContext, name);
    }

    /**
     * Gets a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     *
     * @param  jspContext  may be {@code null}, which will return {@code null}
     */
    public static <T> T get(JspContext jspContext, String name) {
      @SuppressWarnings("unchecked")
      T value = (jspContext == null) ? null : (T) jspContext.getAttribute(name);
      return value;
    }

    /**
     * Gets the value of this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     */
    @Override
    public T get() {
      return get(jspContext, name);
    }

    /**
     * {@linkplain JspContext#getAttribute(java.lang.String) Gets an attribute} in the given scope.
     *
     * @param  jspContext  may be {@code null}, which will return {@code null}
     */
    public static <T> T get(JspContext jspContext, int scope, String name) {
      @SuppressWarnings("unchecked")
      T value = (jspContext == null) ? null : (T) jspContext.getAttribute(name, scope);
      return value;
    }

    /**
     * {@linkplain JspContext#getAttribute(java.lang.String) Gets this attribute} in the given scope.
     */
    public T get(int scope) {
      return get(jspContext, scope, name);
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     *
     * @param  jspContext  may be {@code null}, which will return {@code defaultValue}
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public static <T> T getOrDefault(
        JspContext jspContext,
        String name,
        T defaultValue
    ) {
      @SuppressWarnings("unchecked")
      T value = (jspContext == null) ? null : (T) jspContext.getAttribute(name);
      return (value != null) ? value : defaultValue;
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    @Override
    public T getOrDefault(T defaultValue) {
      return getOrDefault(jspContext, name, defaultValue);
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     *
     * @param  jspContext  may be {@code null}, which will return {@code defaultValue}
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public static <T> T getOrDefault(
        JspContext jspContext,
        int scope,
        String name,
        T defaultValue
    ) {
      @SuppressWarnings("unchecked")
      T value = (jspContext == null) ? null : (T) jspContext.getAttribute(name, scope);
      return (value != null) ? value : defaultValue;
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public T getOrDefault(int scope, T defaultValue) {
      return getOrDefault(jspContext, scope, name, defaultValue);
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T merge(
        JspContext jspContext,
        String name,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      Objects.requireNonNull(value);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name);
        T newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue != oldValue) {
          jspContext.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T merge(
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return merge(jspContext, name, value, remappingFunction);
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T merge(
        JspContext jspContext,
        int scope,
        String name,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      Objects.requireNonNull(value);
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name, scope);
        T newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue != oldValue) {
          jspContext.setAttribute(name, newValue, scope);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public <Ex extends Throwable> T merge(
        int scope,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return merge(jspContext, scope, name, value, remappingFunction);
    }

    /**
     * Removes a {@linkplain JspContext#removeAttribute(java.lang.String, int) page-scope attribute}.
     *
     * @param  jspContext  may be {@code null}, which will skip removal
     */
    public static void remove(JspContext jspContext, String name) {
      if (jspContext != null) {
        jspContext.removeAttribute(name, PageContext.PAGE_SCOPE);
      }
    }

    /**
     * Removes the value from this {@linkplain JspContext#removeAttribute(java.lang.String, int) page-scope attribute}.
     */
    @Override
    public void remove() {
      remove(jspContext, name);
    }

    /**
     * {@linkplain JspContext#removeAttribute(java.lang.String, int) Removes an attribute} from the given scope.
     *
     * @param  jspContext  may be {@code null}, which will skip removal
     */
    public static void remove(JspContext jspContext, int scope, String name) {
      if (jspContext != null) {
        jspContext.removeAttribute(name, scope);
      }
    }

    /**
     * {@linkplain JspContext#removeAttribute(java.lang.String, int) Removes this attribute} from the given scope.
     */
    public void remove(int scope) {
      remove(jspContext, scope, name);
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public static <T> boolean remove(JspContext jspContext, String name, T value) {
      synchronized (jspContext) {
        Object curValue = jspContext.getAttribute(name);
        if (curValue != null && curValue.equals(value)) {
          jspContext.removeAttribute(name, PageContext.PAGE_SCOPE);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean remove(T value) {
      return remove(jspContext, name, value);
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public static <T> boolean remove(JspContext jspContext, int scope, String name, T value) {
      synchronized (jspContext) {
        Object curValue = jspContext.getAttribute(name, scope);
        if (curValue != null && curValue.equals(value)) {
          jspContext.removeAttribute(name, scope);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public boolean remove(int scope, T value) {
      return remove(jspContext, scope, name, value);
    }

    /**
     * Removes an {@linkplain JspContext#removeAttribute(java.lang.String) attribute from all scopes}.
     */
    public static void removeAll(JspContext jspContext, String name) {
      jspContext.removeAttribute(name);
    }

    /**
     * Removes this {@linkplain JspContext#removeAttribute(java.lang.String) attribute from all scopes}.
     */
    public void removeAll() {
      removeAll(jspContext, name);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public static <T> T replace(JspContext jspContext, String name, T value) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) jspContext.getAttribute(name);
        if (curValue != null) {
          jspContext.setAttribute(name, value);
        }
        return curValue;
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    @Override
    public T replace(T value) {
      return replace(jspContext, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public static <T> T replace(JspContext jspContext, int scope, String name, T value) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) jspContext.getAttribute(name, scope);
        if (curValue != null) {
          jspContext.setAttribute(name, value, scope);
        }
        return curValue;
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public T replace(int scope, T value) {
      return replace(jspContext, scope, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public static <T> boolean replace(JspContext jspContext, String name, T oldValue, T newValue) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) jspContext.getAttribute(name);
        if (Objects.equals(curValue, oldValue)) {
          jspContext.setAttribute(name, newValue);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean replace(T oldValue, T newValue) {
      return replace(jspContext, name, oldValue, newValue);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public static <T> boolean replace(JspContext jspContext, int scope, String name, T oldValue, T newValue) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) jspContext.getAttribute(name, scope);
        if (Objects.equals(curValue, oldValue)) {
          jspContext.setAttribute(name, newValue, scope);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public boolean replace(int scope, T oldValue, T newValue) {
      return replace(jspContext, scope, name, oldValue, newValue);
    }

    /**
     * Sets a {@linkplain JspContext#setAttribute(java.lang.String, java.lang.Object) page-scope attribute}.
     */
    public static <T> void set(JspContext jspContext, String name, T value) {
      jspContext.setAttribute(name, value);
    }

    /**
     * Sets the value of this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     */
    @Override
    public void set(T value) {
      set(jspContext, name, value);
    }

    /**
     * {@linkplain JspContext#setAttribute(java.lang.String, java.lang.Object, int) Sets an attribute} in the given scope.
     */
    public static <T> void set(JspContext jspContext, int scope, String name, T value) {
      jspContext.setAttribute(name, value, scope);
    }

    /**
     * {@linkplain JspContext#setAttribute(java.lang.String, java.lang.Object, int) Sets this attribute} in the given scope.
     */
    public void set(int scope, T value) {
      set(jspContext, scope, name, value);
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public static <T> T setIfAbsent(
        JspContext jspContext,
        String name,
        T value
    ) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name);
        if (oldValue == null) {
          jspContext.setAttribute(name, value);
        }
        return oldValue;
      }
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    @Override
    public T setIfAbsent(T value) {
      return setIfAbsent(jspContext, name, value);
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public static <T> T setIfAbsent(
        JspContext jspContext,
        int scope,
        String name,
        T value
    ) {
      synchronized (jspContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) jspContext.getAttribute(name, scope);
        if (oldValue == null) {
          jspContext.setAttribute(name, value, scope);
        }
        return oldValue;
      }
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain JspContext#getAttribute(java.lang.String) page-scope attribute}.
     * Synchronizes on {@link JspContext jspContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public T setIfAbsent(int scope, T value) {
      return setIfAbsent(jspContext, scope, name, value);
    }
  }

  /**
   * A {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
   */
  public static final class Request<T> extends AttributeEE<ServletRequest, T> {

    private final ServletRequest request;

    Request(ServletRequest request, String name) {
      super(name);
      this.request = request;
    }

    /**
     * Gets the {@linkplain ServletRequest#getAttribute(java.lang.String) request context} for this attribute.
     */
    @Override
    public ContextEE.Request getContext() {
      return new ContextEE.Request(request);
    }

    /**
     * Initializes this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute},
     * returning a backup value, which must be {@link OldValue#close() closed} to restore the old value.
     * This is best used in try-with-resources.
     */
    @Override
    public OldValue init(T value) {
      Object oldValue = request.getAttribute(name);
      if (value != oldValue) {
        request.setAttribute(name, value);
      }
      return new OldValue(oldValue) {
        @Override
        public void close() {
          request.setAttribute(name, oldValue);
        }
      };
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T compute(
        ServletRequest request,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) request.getAttribute(name);
        T newValue = remappingFunction.apply(name, oldValue);
        if (newValue != oldValue) {
          request.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T compute(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return compute(request, name, remappingFunction);
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public static <T, Ex extends Throwable> T computeIfAbsent(
        ServletRequest request,
        String name,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T value = (T) request.getAttribute(name);
        if (value == null) {
          value = mappingFunction.apply(name);
          if (value != null) {
            request.setAttribute(name, value);
          }
        }
        return value;
      }
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    @Override
    public <Ex extends Throwable> T computeIfAbsent(
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      return computeIfAbsent(request, name, mappingFunction);
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T computeIfPresent(
        ServletRequest request,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) request.getAttribute(name);
        if (oldValue != null) {
          T newValue = remappingFunction.apply(name, oldValue);
          if (newValue != oldValue) {
            request.setAttribute(name, newValue);
          }
          return newValue;
        } else {
          return null;
        }
      }
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T computeIfPresent(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return computeIfPresent(request, name, remappingFunction);
    }

    /**
     * Gets a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     *
     * @param  request  may be {@code null}, which will return {@code null}
     */
    public static <T> T get(ServletRequest request, String name) {
      @SuppressWarnings("unchecked")
      T value = (request == null) ? null : (T) request.getAttribute(name);
      return value;
    }

    /**
     * Gets the value of this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     */
    @Override
    public T get() {
      return get(request, name);
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     *
     * @param  request  may be {@code null}, which will return {@code defaultValue}
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public static <T> T getOrDefault(
        ServletRequest request,
        String name,
        T defaultValue
    ) {
      @SuppressWarnings("unchecked")
      T value = (request == null) ? null : (T) request.getAttribute(name);
      return (value != null) ? value : defaultValue;
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    @Override
    public T getOrDefault(T defaultValue) {
      return getOrDefault(request, name, defaultValue);
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T merge(
        ServletRequest request,
        String name,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      Objects.requireNonNull(value);
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) request.getAttribute(name);
        T newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue != oldValue) {
          request.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T merge(
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return merge(request, name, value, remappingFunction);
    }

    /**
     * Removes a {@linkplain ServletRequest#removeAttribute(java.lang.String) request-scope attribute}.
     *
     * @param  request  may be {@code null}, which will skip removal
     */
    public static void remove(ServletRequest request, String name) {
      if (request != null) {
        request.removeAttribute(name);
      }
    }

    /**
     * Removes the value from this {@linkplain ServletRequest#removeAttribute(java.lang.String) request-scope attribute}.
     */
    @Override
    public void remove() {
      remove(request, name);
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public static <T> boolean remove(
        ServletRequest request,
        String name,
        T value
    ) {
      synchronized (request) {
        Object curValue = request.getAttribute(name);
        if (curValue != null && curValue.equals(value)) {
          request.removeAttribute(name);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean remove(T value) {
      return remove(request, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public static <T> T replace(
        ServletRequest request,
        String name,
        T value
    ) {
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T curValue = (T) request.getAttribute(name);
        if (curValue != null) {
          request.setAttribute(name, value);
        }
        return curValue;
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    @Override
    public T replace(T value) {
      return replace(request, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public static <T> boolean replace(
        ServletRequest request,
        String name,
        T oldValue,
        T newValue
    ) {
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T curValue = (T) request.getAttribute(name);
        if (Objects.equals(curValue, oldValue)) {
          request.setAttribute(name, newValue);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean replace(T oldValue, T newValue) {
      return replace(request, name, oldValue, newValue);
    }

    /**
     * Sets a {@linkplain ServletRequest#setAttribute(java.lang.String, java.lang.Object) request-scope attribute}.
     */
    public static <T> void set(ServletRequest request, String name, T value) {
      request.setAttribute(name, value);
    }

    /**
     * Sets the value of this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     */
    @Override
    public void set(T value) {
      set(request, name, value);
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public static <T> T setIfAbsent(
        ServletRequest request,
        String name,
        T value
    ) {
      synchronized (request) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) request.getAttribute(name);
        if (oldValue == null) {
          request.setAttribute(name, value);
        }
        return oldValue;
      }
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletRequest#getAttribute(java.lang.String) request-scope attribute}.
     * Synchronizes on {@link ServletRequest request} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    @Override
    public T setIfAbsent(T value) {
      return setIfAbsent(request, name, value);
    }
  }

  /**
   * A {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
   */
  public static final class Session<T> extends AttributeEE<HttpSession, T> {

    private final HttpSession session;

    Session(HttpSession session, String name) {
      super(name);
      this.session = session;
    }

    /**
     * Gets the {@linkplain HttpSession#getAttribute(java.lang.String) session context} for this attribute.
     */
    @Override
    public ContextEE.Session getContext() {
      return new ContextEE.Session(session);
    }

    /**
     * Initializes this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute},
     * returning a backup value, which must be {@link OldValue#close() closed} to restore the old value.
     * This is best used in try-with-resources.
     */
    @Override
    public OldValue init(T value) {
      Object oldValue = (session == null) ? null : session.getAttribute(name);
      if (value != oldValue) {
        session.setAttribute(name, value);
      }
      return new OldValue(oldValue) {
        @Override
        public void close() {
          if (session != null) {
            session.setAttribute(name, oldValue);
          }
        }
      };
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T compute(
        HttpSession session,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (session) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) session.getAttribute(name);
        T newValue = remappingFunction.apply(name, oldValue);
        if (newValue != oldValue) {
          session.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T compute(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return compute(session, name, remappingFunction);
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public static <T, Ex extends Throwable> T computeIfAbsent(
        HttpSession session,
        String name,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      synchronized (session) {
        @SuppressWarnings("unchecked")
        T value = (T) session.getAttribute(name);
        if (value == null) {
          value = mappingFunction.apply(name);
          if (value != null) {
            session.setAttribute(name, value);
          }
        }
        return value;
      }
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    @Override
    public <Ex extends Throwable> T computeIfAbsent(
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      return computeIfAbsent(session, name, mappingFunction);
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T computeIfPresent(
        HttpSession session,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (session) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) session.getAttribute(name);
        if (oldValue != null) {
          T newValue = remappingFunction.apply(name, oldValue);
          if (newValue != oldValue) {
            session.setAttribute(name, newValue);
          }
          return newValue;
        } else {
          return null;
        }
      }
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T computeIfPresent(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return computeIfPresent(session, name, remappingFunction);
    }

    /**
     * Gets a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     *
     * @param  session  may be {@code null}, which will return {@code null}
     */
    public static <T> T get(HttpSession session, String name) {
      @SuppressWarnings("unchecked")
      T value = (session == null) ? null : (T) session.getAttribute(name);
      return value;
    }

    /**
     * Gets the value of this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     */
    @Override
    public T get() {
      return get(session, name);
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     *
     * @param  session  may be {@code null}, which will return {@code defaultValue}
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public static <T> T getOrDefault(
        HttpSession session,
        String name,
        T defaultValue
    ) {
      @SuppressWarnings("unchecked")
      T value = (session == null) ? null : (T) session.getAttribute(name);
      return (value != null) ? value : defaultValue;
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    @Override
    public T getOrDefault(T defaultValue) {
      return getOrDefault(session, name, defaultValue);
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T merge(
        HttpSession session,
        String name,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      Objects.requireNonNull(value);
      synchronized (session) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) session.getAttribute(name);
        T newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue != oldValue) {
          session.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T merge(
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return merge(session, name, value, remappingFunction);
    }

    /**
     * Removes a {@linkplain HttpSession#removeAttribute(java.lang.String) session-scope attribute}.
     *
     * @param  session  may be {@code null}, which will skip removal
     */
    public static void remove(HttpSession session, String name) {
      if (session != null) {
        session.removeAttribute(name);
      }
    }

    /**
     * Removes the value from this {@linkplain HttpSession#removeAttribute(java.lang.String) session-scope attribute}.
     */
    @Override
    public void remove() {
      remove(session, name);
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public static <T> boolean remove(
        HttpSession session,
        String name,
        T value
    ) {
      if (session == null) {
        return false;
      } else {
        synchronized (session) {
          Object curValue = session.getAttribute(name);
          if (curValue != null && curValue.equals(value)) {
            session.removeAttribute(name);
            return true;
          } else {
            return false;
          }
        }
      }
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean remove(T value) {
      return remove(session, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public static <T> T replace(
        HttpSession session,
        String name,
        T value
    ) {
      if (session == null) {
        return null;
      } else {
        synchronized (session) {
          @SuppressWarnings("unchecked")
          T curValue = (T) session.getAttribute(name);
          if (curValue != null) {
            session.setAttribute(name, value);
          }
          return curValue;
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    @Override
    public T replace(T value) {
      return replace(session, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public static <T> boolean replace(
        HttpSession session,
        String name,
        T oldValue,
        T newValue
    ) {
      if (session == null && oldValue == null && newValue == null) {
        return true;
      } else {
        synchronized (session) {
          @SuppressWarnings({"unchecked", "null"})
          T curValue = (T) session.getAttribute(name);
          if (Objects.equals(curValue, oldValue)) {
            session.setAttribute(name, newValue);
            return true;
          } else {
            return false;
          }
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean replace(T oldValue, T newValue) {
      return replace(session, name, oldValue, newValue);
    }

    /**
     * Sets a {@linkplain HttpSession#setAttribute(java.lang.String, java.lang.Object) session-scope attribute}.
     */
    @SuppressWarnings("null")
    public static <T> void set(HttpSession session, String name, T value) {
      if (session != null || value != null) {
        session.setAttribute(name, value);
      }
    }

    /**
     * Sets the value of this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     */
    @Override
    public void set(T value) {
      set(session, name, value);
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for a {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public static <T> T setIfAbsent(
        HttpSession session,
        String name,
        T value
    ) {
      if (session == null && value == null) {
        return null;
      } else {
        synchronized (session) {
          @SuppressWarnings({"unchecked", "null"})
          T oldValue = (T) session.getAttribute(name);
          if (oldValue == null) {
            session.setAttribute(name, value);
          }
          return oldValue;
        }
      }
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain HttpSession#getAttribute(java.lang.String) session-scope attribute}.
     * Synchronizes on {@link HttpSession session} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    @Override
    public T setIfAbsent(T value) {
      return setIfAbsent(session, name, value);
    }
  }

  /**
   * An {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
   */
  public static final class Application<T> extends AttributeEE<ServletContext, T> {

    private final ServletContext servletContext;

    Application(ServletContext servletContext, String name) {
      super(name);
      this.servletContext = servletContext;
    }

    /**
     * Gets the {@linkplain ServletContext#getAttribute(java.lang.String) application context} for this attribute.
     */
    @Override
    public ContextEE.Application getContext() {
      return new ContextEE.Application(servletContext);
    }

    /**
     * Initializes this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute},
     * returning a backup value, which must be {@link OldValue#close() closed} to restore the old value.
     * This is best used in try-with-resources.
     */
    @Override
    public OldValue init(T value) {
      Object oldValue = servletContext.getAttribute(name);
      if (value != oldValue) {
        servletContext.setAttribute(name, value);
      }
      return new OldValue(oldValue) {
        @Override
        public void close() {
          servletContext.setAttribute(name, oldValue);
        }
      };
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T compute(
        ServletContext servletContext,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) servletContext.getAttribute(name);
        T newValue = remappingFunction.apply(name, oldValue);
        if (newValue != oldValue) {
          servletContext.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#compute(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#compute(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T compute(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return compute(servletContext, name, remappingFunction);
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    public static <T, Ex extends Throwable> T computeIfAbsent(
        ServletContext servletContext,
        String name,
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T value = (T) servletContext.getAttribute(name);
        if (value == null) {
          value = mappingFunction.apply(name);
          if (value != null) {
            servletContext.setAttribute(name, value);
          }
        }
        return value;
      }
    }

    /**
     * Much like {@link Map#computeIfAbsent(java.lang.Object, java.util.function.Function)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#computeIfAbsent(java.lang.Object, java.util.function.Function)
     */
    @Override
    public <Ex extends Throwable> T computeIfAbsent(
        FunctionE<? super String, ? extends T, ? extends Ex> mappingFunction
    ) throws Ex {
      return computeIfAbsent(servletContext, name, mappingFunction);
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T computeIfPresent(
        ServletContext servletContext,
        String name,
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) servletContext.getAttribute(name);
        if (oldValue != null) {
          T newValue = remappingFunction.apply(name, oldValue);
          if (newValue != oldValue) {
            servletContext.setAttribute(name, newValue);
          }
          return newValue;
        } else {
          return null;
        }
      }
    }

    /**
     * Much like {@link Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#computeIfPresent(java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T computeIfPresent(
        BiFunctionE<? super String, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return computeIfPresent(servletContext, name, remappingFunction);
    }

    /**
     * Gets an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     *
     * @param  servletContext  may be {@code null}, which will return {@code null}
     */
    public static <T> T get(ServletContext servletContext, String name) {
      @SuppressWarnings("unchecked")
      T value = (servletContext == null) ? null : (T) servletContext.getAttribute(name);
      return value;
    }

    /**
     * Gets the value of this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     */
    @Override
    public T get() {
      return get(servletContext, name);
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     *
     * @param  servletContext  may be {@code null}, which will return {@code defaultValue}
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    public static <T> T getOrDefault(
        ServletContext servletContext,
        String name,
        T defaultValue
    ) {
      @SuppressWarnings("unchecked")
      T value = (servletContext == null) ? null : (T) servletContext.getAttribute(name);
      return (value != null) ? value : defaultValue;
    }

    /**
     * Much like {@link Map#getOrDefault(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     *
     * @see  Map#getOrDefault(java.lang.Object, java.lang.Object)
     */
    @Override
    public T getOrDefault(T defaultValue) {
      return getOrDefault(servletContext, name, defaultValue);
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    public static <T, Ex extends Throwable> T merge(
        ServletContext servletContext,
        String name,
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      Objects.requireNonNull(remappingFunction);
      Objects.requireNonNull(value);
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) servletContext.getAttribute(name);
        T newValue = (oldValue == null) ? value : remappingFunction.apply(oldValue, value);
        if (newValue != oldValue) {
          servletContext.setAttribute(name, newValue);
        }
        return newValue;
      }
    }

    /**
     * Much like {@link Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#merge(java.lang.Object, java.lang.Object, java.util.function.BiFunction)
     */
    @Override
    public <Ex extends Throwable> T merge(
        T value,
        BiFunctionE<? super T, ? super T, ? extends T, ? extends Ex> remappingFunction
    ) throws Ex {
      return merge(servletContext, name, value, remappingFunction);
    }

    /**
     * Removes an {@linkplain ServletContext#removeAttribute(java.lang.String) application-scope attribute}.
     *
     * @param  servletContext  may be {@code null}, which will skip removal
     */
    public static void remove(ServletContext servletContext, String name) {
      if (servletContext != null) {
        servletContext.removeAttribute(name);
      }
    }

    /**
     * Removes the value from this {@linkplain ServletContext#removeAttribute(java.lang.String) application-scope attribute}.
     */
    @Override
    public void remove() {
      remove(servletContext, name);
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    public static <T> boolean remove(
        ServletContext servletContext,
        String name,
        T value
    ) {
      synchronized (servletContext) {
        Object curValue = servletContext.getAttribute(name);
        if (curValue != null && curValue.equals(value)) {
          servletContext.removeAttribute(name);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#remove(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#remove(java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean remove(T value) {
      return remove(servletContext, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    public static <T> T replace(
        ServletContext servletContext,
        String name,
        T value
    ) {
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) servletContext.getAttribute(name);
        if (curValue != null) {
          servletContext.setAttribute(name, value);
        }
        return curValue;
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object)
     */
    @Override
    public T replace(T value) {
      return replace(servletContext, name, value);
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    public static <T> boolean replace(
        ServletContext servletContext,
        String name,
        T oldValue,
        T newValue
    ) {
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T curValue = (T) servletContext.getAttribute(name);
        if (Objects.equals(curValue, oldValue)) {
          servletContext.setAttribute(name, newValue);
          return true;
        } else {
          return false;
        }
      }
    }

    /**
     * Much like {@link Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#replace(java.lang.Object, java.lang.Object, java.lang.Object)
     */
    @Override
    public boolean replace(T oldValue, T newValue) {
      return replace(servletContext, name, oldValue, newValue);
    }

    /**
     * Sets an {@linkplain ServletContext#setAttribute(java.lang.String, java.lang.Object) application-scope attribute}.
     */
    public static <T> void set(ServletContext servletContext, String name, T value) {
      servletContext.setAttribute(name, value);
    }

    /**
     * Sets the value of this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     */
    @Override
    public void set(T value) {
      set(servletContext, name, value);
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for an {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    public static <T> T setIfAbsent(
        ServletContext servletContext,
        String name,
        T value
    ) {
      synchronized (servletContext) {
        @SuppressWarnings("unchecked")
        T oldValue = (T) servletContext.getAttribute(name);
        if (oldValue == null) {
          servletContext.setAttribute(name, value);
        }
        return oldValue;
      }
    }

    /**
     * Much like {@link Map#putIfAbsent(java.lang.Object, java.lang.Object)},
     * but for this {@linkplain ServletContext#getAttribute(java.lang.String) application-scope attribute}.
     * Synchronizes on {@link ServletContext servletContext} to ensure atomic operation.
     *
     * @see  Map#putIfAbsent(java.lang.Object, java.lang.Object)
     */
    @Override
    public T setIfAbsent(T value) {
      return setIfAbsent(servletContext, name, value);
    }
  }

  // <editor-fold desc="Name">
  /**
   * A name without any specific scope or context.
   * <p>
   * {@link AttributeEE}: Has name, still needs scope or context.
   * </p>
   */
  public static class Name<T> extends com.aoapps.lang.attribute.Attribute.Name<T> {

    private static final long serialVersionUID = 1L;

    private Name(String name) {
      super(name);
    }

    // <editor-fold desc="Scope">
    /**
     * Supports scope attributes in scopes of contexts types {@link ServletContext}, {@link ServletRequest},
     * {@link HttpSession}, and {@link JspContext}.
     *
     * @see  com.aoapps.lang.attribute.Attribute.Name#scope(java.lang.Class)
     */
    public static class ScopeEEFactory<C, T> implements ScopeFactory<C, T> {
      @Override
      @SuppressWarnings("unchecked")
      public ScopeEE.Attribute<C, T> attribute(Class<?> contextType, String name) {
        if (contextType == JspContext.class) {
          return (ScopeEE.Attribute<C, T>) new ScopeEE.Page.Attribute<>(name);
        }
        if (contextType == ServletRequest.class) {
          return (ScopeEE.Attribute<C, T>) new ScopeEE.Request.Attribute<>(name);
        }
        if (contextType == HttpSession.class) {
          return (ScopeEE.Attribute<C, T>) new ScopeEE.Session.Attribute<>(name);
        }
        if (contextType == ServletContext.class) {
          return (ScopeEE.Attribute<C, T>) new ScopeEE.Application.Attribute<>(name);
        }
        return null;
      }
    }

    /**
     * {@link AttributeEE}: Uses the given scope (located by content type) and this name, still needs context.
     *
     * @param  contextType  The context type must be one of {@link ServletContext}, {@link ServletRequest}, {@link HttpSession},
     *                      {@link JspContext}, or a type registered as {@link ScopeFactory}
     *                      that returns a instance of {@link ScopeEE.Attribute}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C> ScopeEE.Attribute<C, T> scope(Class<C> contextType) {
      // Avoid ServiceLoader for expected types
      if (contextType == JspContext.class) {
        return (ScopeEE.Attribute<C, T>) page();
      }
      if (contextType == ServletRequest.class) {
        return (ScopeEE.Attribute<C, T>) request();
      }
      if (contextType == HttpSession.class) {
        return (ScopeEE.Attribute<C, T>) session();
      }
      if (contextType == ServletContext.class) {
        return (ScopeEE.Attribute<C, T>) application();
      }
      // Check for additional types registered with ServiceLoader
      Scope.Attribute<C, T> attribute = super.scope(contextType);
      if (attribute instanceof ScopeEE.Attribute) {
        return (ScopeEE.Attribute<C, T>) attribute;
      }
      throw new IllegalArgumentException(
          "Scope attribute is not an instance of " + ScopeEE.Attribute.class.getName() + ".  To use attributes of any type, use "
              + com.aoapps.lang.attribute.Attribute.class.getName() + ".scope(contextType) instead.  Context type is \""
              + (contextType == null ? "null" : contextType.toGenericString()) + "\""
      );
    }

    /**
     * {@link AttributeEE}: Uses the given scope and this name, still needs context.
     */
    @SuppressWarnings("unchecked")
    public <C> ScopeEE.Attribute<C, T> scope(ScopeEE<C> scope) {
      if (scope == ScopeEE.PAGE) {
        return (ScopeEE.Attribute<C, T>) application();
      }
      if (scope == ScopeEE.REQUEST) {
        return (ScopeEE.Attribute<C, T>) request();
      }
      if (scope == ScopeEE.SESSION) {
        return (ScopeEE.Attribute<C, T>) session();
      }
      if (scope == ScopeEE.APPLICATION) {
        return (ScopeEE.Attribute<C, T>) application();
      }
      return scope.attribute(name);
    }

    /**
     * {@link AttributeEE}: Uses the given scope and this name, still needs context.
     *
     * @param  scope  Must be an instance of {@link ScopeEE}
     */
    @Override
    public <C> ScopeEE.Attribute<C, T> scope(Scope<C> scope) {
      return scope((ScopeEE<C>) scope);
    }

    /**
     * {@link AttributeEE}: Uses the {@linkplain ServletContext#getAttribute(java.lang.String) application scope}
     * and this name, still needs context.
     */
    public ScopeEE.Application.Attribute<T> application() {
      return new ScopeEE.Application.Attribute<>(name);
    }

    /**
     * {@link AttributeEE}: Uses the {@linkplain ServletRequest#getAttribute(java.lang.String) request scope}
     * and this name, still needs context.
     */
    public ScopeEE.Request.Attribute<T> request() {
      return new ScopeEE.Request.Attribute<>(name);
    }

    /**
     * {@link AttributeEE}: Uses the {@linkplain HttpSession#getAttribute(java.lang.String) session scope}
     * and this name, still needs context.
     */
    public ScopeEE.Session.Attribute<T> session() {
      return new ScopeEE.Session.Attribute<>(name);
    }

    /**
     * {@link AttributeEE}: Uses the {@linkplain JspContext#getAttribute(java.lang.String) page scope}
     * and this name, still needs context.
     */
    public ScopeEE.Page.Attribute<T> page() {
      return new ScopeEE.Page.Attribute<>(name);
    }

    // </editor-fold>

    // <editor-fold desc="Context">
    /**
     * Supports attributes in contexts of types {@link ServletContext}, {@link ServletRequest},
     * {@link HttpSession}, and {@link JspContext}.
     *
     * @see  com.aoapps.lang.attribute.Attribute.Name#context(java.lang.Object)
     */
    public static class ContextEEFactory<C, T> implements ContextFactory<C, T> {
      @Override
      @SuppressWarnings("unchecked")
      public AttributeEE<C, T> attribute(Object context, String name) {
        if (context instanceof JspContext) {
          return (AttributeEE<C, T>) new Page<>((JspContext) context, name);
        }
        if (context instanceof ServletRequest) {
          return (AttributeEE<C, T>) new Request<>((ServletRequest) context, name);
        }
        if (context instanceof HttpSession) {
          return (AttributeEE<C, T>) new Session<>((HttpSession) context, name);
        }
        if (context instanceof ServletContext) {
          return (AttributeEE<C, T>) new Application<>((ServletContext) context, name);
        }
        return null;
      }
    }

    /**
     * {@link AttributeEE}: Uses the given context and this name.
     *
     * @param  <C>  The context must be one of {@link ServletContext}, {@link ServletRequest}, {@link HttpSession},
     *              {@link JspContext}, or a type registered as {@link ContextFactory}
     *              that returns a instance of {@link AttributeEE}.
     */
    @Override
    @SuppressWarnings("unchecked")
    public <C> AttributeEE<C, T> context(C context) {
      // Avoid ServiceLoader for expected types
      if (context instanceof JspContext) {
        return (AttributeEE<C, T>) context((JspContext) context);
      }
      if (context instanceof ServletRequest) {
        return (AttributeEE<C, T>) context((ServletRequest) context);
      }
      if (context instanceof HttpSession) {
        return (AttributeEE<C, T>) context((HttpSession) context);
      }
      if (context instanceof ServletContext) {
        return (AttributeEE<C, T>) context((ServletContext) context);
      }
      // Check for additional types registered with ServiceLoader
      Attribute<C, T> attribute = super.context(context);
      if (attribute instanceof AttributeEE) {
        return (AttributeEE<C, T>) attribute;
      }
      throw new IllegalArgumentException(
          "Attribute is not an instance of " + AttributeEE.class.getName() + ".  To use attributes of any type, use "
              + com.aoapps.lang.attribute.Attribute.class.getName() + ".context(context) instead.  Context is type \""
              + (context == null ? "null" : context.getClass().toGenericString()) + "\": " + context
      );
    }

    /**
     * {@link AttributeEE}: Uses the given {@linkplain ServletContext#getAttribute(java.lang.String) application context}.
     */
    public Application<T> context(ServletContext servletContext) {
      return new Application<>(servletContext, name);
    }

    /**
     * {@link AttributeEE}: Uses the given {@linkplain ServletRequest#getAttribute(java.lang.String) request context}.
     */
    public Request<T> context(ServletRequest request) {
      return new Request<>(request, name);
    }

    /**
     * {@link AttributeEE}: Uses the given {@linkplain HttpSession#getAttribute(java.lang.String) session context}.
     */
    public Session<T> context(HttpSession session) {
      return new Session<>(session, name);
    }

    /**
     * {@link AttributeEE}: Uses the given {@linkplain JspContext#getAttribute(java.lang.String) page context}.
     */
    public Page<T> context(JspContext jspContext) {
      return new Page<>(jspContext, name);
    }
    // </editor-fold>
  }

  /**
   * {@link AttributeEE}: Uses the given name, still needs scope or context.
   */
  public static <T> Name<T> attribute(String name) {
    return new Name<>(name);
  }

  // </editor-fold>

  // <editor-fold desc="JSTL 1.2">
  /**
   * A {@linkplain Config JSTL attribute name} without any specific scope or context.  Suffixes are automatically
   * added to be compatible with {@link Config#set(javax.servlet.jsp.PageContext, java.lang.String, java.lang.Object, int)}.
   * <p>
   * {@link AttributeEE}: Has name, still needs scope or context.
   * </p>
   */
  public static class Jstl<T> extends Name<T> {

    /**
     * @see  Config#PAGE_SCOPE_SUFFIX
     */
    private static final String PAGE_SCOPE_SUFFIX = ".page";

    /**
     * @see  Config#REQUEST_SCOPE_SUFFIX
     */
    private static final String REQUEST_SCOPE_SUFFIX = ".request";

    /**
     * @see  Config#SESSION_SCOPE_SUFFIX
     */
    private static final String SESSION_SCOPE_SUFFIX = ".session";

    /**
     * @see  Config#APPLICATION_SCOPE_SUFFIX
     */
    private static final String APPLICATION_SCOPE_SUFFIX = ".application";

    private static final long serialVersionUID = 1L;

    private Jstl(String name) {
      super(name);
    }

    // <editor-fold desc="Scope">
    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#APPLICATION_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public ScopeEE.Application.Attribute<T> application() {
      return new ScopeEE.Application.Attribute<>(name + APPLICATION_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#REQUEST_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public ScopeEE.Request.Attribute<T> request() {
      return new ScopeEE.Request.Attribute<>(name + REQUEST_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#SESSION_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public ScopeEE.Session.Attribute<T> session() {
      return new ScopeEE.Session.Attribute<>(name + SESSION_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#PAGE_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public ScopeEE.Page.Attribute<T> page() {
      return new ScopeEE.Page.Attribute<>(name + PAGE_SCOPE_SUFFIX);
    }

    // </editor-fold>

    // <editor-fold desc="Context">
    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#APPLICATION_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public Application<T> context(ServletContext servletContext) {
      return new Application<>(servletContext, name + APPLICATION_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#REQUEST_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public Request<T> context(ServletRequest request) {
      return new Request<>(request, name + REQUEST_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#SESSION_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public Session<T> context(HttpSession session) {
      return new Session<>(session, name + SESSION_SCOPE_SUFFIX);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Automatically appends {@link Config#PAGE_SCOPE_SUFFIX} to name.
     * </p>
     */
    @Override
    public Page<T> context(JspContext jspContext) {
      return new Page<>(jspContext, name + PAGE_SCOPE_SUFFIX);
    }

    /**
     * @see  Config#FMT_LOCALE
     */
    public static final Jstl<Locale> FMT_LOCALE = new Jstl<>(Config.FMT_LOCALE);

    /**
     * @see  Config#FMT_FALLBACK_LOCALE
     */
    public static final Jstl<Locale> FMT_FALLBACK_LOCALE = new Jstl<>(Config.FMT_FALLBACK_LOCALE);

    /**
     * @see  Config#FMT_LOCALIZATION_CONTEXT
     */
    public static final Jstl<LocalizationContext> FMT_LOCALIZATION_CONTEXT = new Jstl<>(Config.FMT_LOCALIZATION_CONTEXT);

    /**
     * @see  Config#FMT_TIME_ZONE
     */
    public static final Jstl<TimeZone> FMT_TIME_ZONE = new Jstl<>(Config.FMT_TIME_ZONE);

    /**
     * @see  Config#SQL_DATA_SOURCE
     */
    // TODO: String or DataSource? https://flylib.com/books/en/2.521.1.92/1/
    public static final Jstl<DataSource> SQL_DATA_SOURCE = new Jstl<>(Config.SQL_DATA_SOURCE);

    /**
     * @see  Config#SQL_MAX_ROWS
     */
    // TODO: String or Integer? https://flylib.com/books/en/2.521.1.92/1/
    public static final Jstl<Integer> SQL_MAX_ROWS = new Jstl<>(Config.SQL_MAX_ROWS);
    // </editor-fold>
  }

  /**
   * A {@linkplain Config JSTL attribute name} without any specific scope or context.  Suffixes are automatically
   * added to be compatible with {@link Config#set(javax.servlet.jsp.PageContext, java.lang.String, java.lang.Object, int)}.
   * <p>
   * {@link AttributeEE}: Uses the given name, still needs scope or context.
   * </p>
   */
  public static <T> Jstl<T> jstl(String name) {
    return new Jstl<>(name);
  }
  // </editor-fold>
}
