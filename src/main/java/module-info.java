/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2021, 2022, 2023  AO Industries, Inc.
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
module com.aoapps.servlet.util {
  exports com.aoapps.servlet;
  exports com.aoapps.servlet.attribute;
  exports com.aoapps.servlet.el;
  exports com.aoapps.servlet.function;
  exports com.aoapps.servlet.http;
  exports com.aoapps.servlet.i18n;
  exports com.aoapps.servlet.jsp;
  exports com.aoapps.servlet.jsp.function;
  exports com.aoapps.servlet.jsp.tagext;
  provides com.aoapps.lang.CoercionOptimizerInitializer with com.aoapps.servlet.BodyContentImplCoercionOptimizerInitializer;
  provides com.aoapps.lang.ThrowableSurrogateFactoryInitializer with com.aoapps.servlet.JavaeeWebSurrogateFactoryInitializer;
  provides com.aoapps.lang.attribute.Attribute.Name.ContextFactory with com.aoapps.servlet.attribute.AttributeEE.Name.ContextEEFactory;
  provides com.aoapps.lang.attribute.Attribute.Name.ScopeFactory with com.aoapps.servlet.attribute.AttributeEE.Name.ScopeEEFactory;
  // Direct
  requires com.aoapps.collections; // <groupId>com.aoapps</groupId><artifactId>ao-collections</artifactId>
  requires com.aoapps.hodgepodge; // <groupId>com.aoapps</groupId><artifactId>ao-hodgepodge</artifactId>
  requires com.aoapps.lang; // <groupId>com.aoapps</groupId><artifactId>ao-lang</artifactId>
  requires com.aoapps.net.types; // <groupId>com.aoapps</groupId><artifactId>ao-net-types</artifactId>
  requires javax.el.api; // <groupId>javax.el</groupId><artifactId>javax.el-api</artifactId>
  requires javax.servlet.api; // <groupId>javax.servlet</groupId><artifactId>javax.servlet-api</artifactId>
  requires javax.servlet.jsp.api; // <groupId>javax.servlet.jsp</groupId><artifactId>javax.servlet.jsp-api</artifactId>
  requires static taglibs.standard.impl; // <groupId>org.apache.taglibs</groupId><artifactId>taglibs-standard-impl</artifactId>
  requires static taglibs.standard.spec; // <groupId>org.apache.taglibs</groupId><artifactId>taglibs-standard-spec</artifactId>
  // Java SE
  requires java.logging;
  requires java.sql;
}
