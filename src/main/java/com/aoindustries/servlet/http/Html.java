/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2019  AO Industries, Inc.
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
 * along with ao-servlet-util.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.servlet.http;

import com.aoindustries.encoding.ChainWriter;
import com.aoindustries.encoding.Coercion;
import com.aoindustries.encoding.EncodingContext;
import static com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder.javaScriptInXhtmlAttributeEncoder;
import com.aoindustries.encoding.MediaEncoder;
import com.aoindustries.encoding.MediaException;
import com.aoindustries.encoding.MediaType;
import com.aoindustries.encoding.MediaWriter;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.textInXhtmlAttributeEncoder;
import static com.aoindustries.encoding.TextInXhtmlEncoder.textInXhtmlEncoder;
import com.aoindustries.encoding.servlet.HttpServletResponseEncodingContext;
import com.aoindustries.io.NoCloseWriter;
import com.aoindustries.lang.LocalizedIllegalArgumentException;
import com.aoindustries.lang.LocalizedIllegalStateException;
import static com.aoindustries.servlet.http.ApplicationResources.accessor;
import com.aoindustries.util.StringUtility;
import com.aoindustries.util.WrappedException;
import com.aoindustries.util.i18n.MarkupType;
import java.io.IOException;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.servlet.ServletContext;
import javax.servlet.ServletRequest;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Static utilities that may be useful for HTML/XHTML in servlet/JSP/taglib environments.
 * See also <a href="https://github.com/xmlet/HtmlFlow">HtmlFlow</a>.
 *
 * @author  AO Industries, Inc.
 */
public class Html {

	/**
	 * Content type for HTML.
	 */
	public static final String CONTENT_TYPE_HTML = "text/html";

	/**
	 * Content type for XHTML.
	 */
	public static final String CONTENT_TYPE_XHTML = "application/xhtml+xml";

	/**
	 * The default, and recommended, encoding for (X)HTML.
	 */
	public static final Charset ENCODING = StandardCharsets.UTF_8;

	public enum Serialization {
		SGML {
			@Override
			public String getContentType() {
				return CONTENT_TYPE_HTML;
			}

			@Override
			public String getSelfClose() {
				return ">";
			}

			@Override
			public Serialization selfClose(Appendable out) throws IOException {
				out.append('>');
				return this;
			}
		},
		XML {
			@Override
			public String getContentType() {
				return CONTENT_TYPE_XHTML;
			}

			@Override
			public String getSelfClose() {
				return " />";
			}
		};

		/**
		 * Gets the content-type header to use for this serialization.
		 */
		abstract public String getContentType();

		/**
		 * Gets the self-closing tag characters.
		 */
		abstract public String getSelfClose();

		/**
		 * Appends the self-closing tag characters.
		 */
		public Serialization selfClose(Appendable out) throws IOException {
			out.append(getSelfClose());
			return this;
		}

		/**
		 * Context init parameter that may be used to configure the use of XHTML within an application.
		 * Must be one of "SGML", "XML", or "auto" (the default).
		 */
		public static final String DEFAULT_INIT_PARAM = Serialization.class.getName() + ".default";

		/**
		 * Determine if the content may be served as <code>application/xhtml+xml</code> by the
		 * rules defined in <a href="http://www.w3.org/TR/xhtml-media-types/">http://www.w3.org/TR/xhtml-media-types/</a>
		 * Default to <code>application/xhtml+xml</code> as discussed at
		 * <a href="https://web.archive.org/web/20080913043830/http://www.smackthemouse.com/xhtmlxml">http://www.smackthemouse.com/xhtmlxml</a>
		 */
		public static Serialization getDefault(ServletContext servletContext, HttpServletRequest request) {
			String initParam = servletContext.getInitParameter(DEFAULT_INIT_PARAM);
			if(initParam != null) {
				initParam = initParam.trim();
				if(!initParam.isEmpty()) {
					if("SGML".equalsIgnoreCase(initParam)) {
						return SGML;
					} else if("XML".equalsIgnoreCase(initParam)) {
						return XML;
					} else if(!"auto".equalsIgnoreCase(initParam)) {
						throw new IllegalArgumentException("Unexpected value for " + DEFAULT_INIT_PARAM + ": Must be one of \"SGML\", \"XML\", or \"auto\": " + initParam);
					}
				}
			}
			// Some test accept headers:
			//   Firefox: text/xml,application/xml,application/xhtml+xml,text/html;q=0.9,text/plain;q=0.8,image/png,*/*;q=0.5
			//   IE 6: */*
			//   IE 8: */*
			//   IE 8 Compat: */*
			@SuppressWarnings("unchecked")
			Enumeration<String> acceptValues = request.getHeaders("Accept");

			boolean hasAcceptHeader = false;
			boolean hasAcceptApplicationXhtmlXml = false;
			boolean hasAcceptTextHtml = false;
			boolean hasAcceptStarStar = false;
			if(acceptValues != null) {
				while(acceptValues.hasMoreElements()) {
					hasAcceptHeader = true;
					for(String value : StringUtility.splitString(acceptValues.nextElement(), ',')) {
						value = value.trim();
						final List<String> params = StringUtility.splitString(value, ';');
						final int paramsSize = params.size();
						if(paramsSize > 0) {
							String acceptType = params.get(0).trim();
							if(acceptType.equals("*/*")) {
								// No q parameter parsing for */*
								hasAcceptStarStar = true;
							} else if(
								// Parse and check the q for these two types
								acceptType.equalsIgnoreCase(CONTENT_TYPE_XHTML)
								|| acceptType.equalsIgnoreCase(CONTENT_TYPE_HTML)
							) {
								// Find any q value
								boolean hasNegativeQ = false;
								for(int paramNum = 1; paramNum < paramsSize; paramNum++) {
									String paramSet = params.get(paramNum).trim();
									if(paramSet.startsWith("q=") || paramSet.startsWith("Q=")) {
										try {
											float q = Float.parseFloat(paramSet.substring(2).trim());
											if(q < 0) {
												hasNegativeQ = true;
												break;
											}
										} catch(NumberFormatException err) {
											// Intentionally ignored
										}
									}
								}
								if(!hasNegativeQ) {
									if(acceptType.equalsIgnoreCase(CONTENT_TYPE_XHTML)) hasAcceptApplicationXhtmlXml = true;
									else if(acceptType.equalsIgnoreCase(CONTENT_TYPE_HTML)) hasAcceptTextHtml = true;
									else throw new AssertionError("Unexpected value for acceptType: " + acceptType);
								}
							}
						}
					}
				}
			}
			// If the Accept header explicitly contains application/xhtml+xml  (with either no "q" parameter or a positive "q" value) deliver the document using that media type.
			if(hasAcceptApplicationXhtmlXml) return XML;
			// If the Accept header explicitly contains text/html  (with either no "q" parameter or a positive "q" value) deliver the document using that media type.
			if(hasAcceptTextHtml) return SGML;
			// If the accept header contains "*/*" (a convention some user agents use to indicate that they will accept anything), deliver the document using text/html.
			if(hasAcceptStarStar) return SGML;
			// If has no accept headers
			if(!hasAcceptHeader) return XML;
			// This choice is not clear from either of the cited documents.  If there is an accept line,
			// and it doesn't have */* or application/xhtml+xml or text/html, we'll serve as text/html
			// since it is a fairly broken client anyway and would be even less likely to know xhtml.
			return SGML;
		}

		private static final String REQUEST_ATTRIBUTE_NAME = Serialization.class.getName();

		/**
		 * Registers the serialization in effect for the request.
		 */
		public static void set(ServletRequest request, Serialization serialization) {
			request.setAttribute(REQUEST_ATTRIBUTE_NAME, serialization);
		}

		/**
		 * Replaces the serialization in effect for the request.
		 *
		 * @return  The previous attribute value, if any
		 */
		public static Serialization replace(ServletRequest request, Serialization serialization) {
			Serialization old = (Serialization)request.getAttribute(REQUEST_ATTRIBUTE_NAME);
			request.setAttribute(REQUEST_ATTRIBUTE_NAME, serialization);
			return old;
		}

		/**
		 * Gets the serialization in effect for the request, or {@linkplain #getDefault(javax.servlet.ServletContext, javax.servlet.http.HttpServletRequest) the default}
		 * when not yet {@linkplain #set(javax.servlet.ServletRequest, com.aoindustries.servlet.http.Html.Serialization) set}.
		 * <p>
		 * Once the default is resolved,
		 * {@linkplain #set(javax.servlet.ServletRequest, com.aoindustries.servlet.http.Html.Serialization) sets the request attribute}.
		 * </p>
		 */
		public static Serialization get(ServletContext servletContext, HttpServletRequest request) {
			Serialization serialization = (Serialization)request.getAttribute(REQUEST_ATTRIBUTE_NAME);
			if(serialization == null) {
				serialization = getDefault(servletContext, request);
				request.setAttribute(REQUEST_ATTRIBUTE_NAME, serialization);
			}
			return serialization;
		}
	}

	public enum Doctype {
		// See http://www.ibm.com/developerworks/library/x-think45/
		HTML5 {
			@Override
			public String getDoctype(Serialization serialization) {
				return "<!DOCTYPE html>\n";
			}
			@Override
			public String getScriptType() {
				return "";
			}
			@Override
			public Doctype scriptType(Appendable out) throws IOException {
				// Do nothing
				return this;
			}
			@Override
			public String getStyleType() {
				return "";
			}
			@Override
			public Doctype styleType(Appendable out) throws IOException {
				// Do nothing
				return this;
			}
		},
		STRICT {
			@Override
			public String getDoctype(Serialization serialization) {
				switch(serialization) {
					case SGML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
					case XML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
			@Override
			public String getScriptType() {
				return " type=\"text/javascript\"";
			}
			@Override
			public String getStyleType() {
				return " type=\"" + Style.Type.TEXT_CSS.getContentType() + "\"";
			}
			@Override
			public Doctype styleType(Appendable out) throws IOException {
				out.append(" type=\"");
				out.append(Style.Type.TEXT_CSS.getContentType());
				out.append('"');
				return this;
			}
		},
		TRANSITIONAL {
			@Override
			public String getDoctype(Serialization serialization) {
				switch(serialization) {
					case SGML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n";
					case XML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
			@Override
			public String getScriptType() {
				return STRICT.getScriptType();
			}
			@Override
			public String getStyleType() {
				return STRICT.getStyleType();
			}
		},
		FRAMESET {
			@Override
			public String getDoctype(Serialization serialization) {
				switch(serialization) {
					case SGML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">\n";
					case XML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
			@Override
			public String getScriptType() {
				return STRICT.getScriptType();
			}
			@Override
			public String getStyleType() {
				return STRICT.getStyleType();
			}
		},
		NONE {
			@Override
			public String getXmlDeclaration(Serialization serialization, String documentEncoding) {
				return "";
			}
			@Override
			public Doctype xmlDeclaration(Serialization serialization, String documentEncoding, Appendable out) {
				// Do nothing
				return this;
			}
			@Override
			public String getDoctype(Serialization serialization) {
				return "";
			}
			@Override
			public Doctype doctype(Serialization serialization, Appendable out) throws IOException {
				// Do nothing
				return this;
			}
			@Override
			public String getScriptType() {
				// Very old doctype-less, support IE6: http://www.javascriptkit.com/javatutors/languageattri3.shtml
				return " language=\"JavaScript1.3\"";
			}
			@Override
			public String getStyleType() {
				return STRICT.getStyleType();
			}
		};

		private static boolean isUTF8(String documentEncoding) {
			return
				StandardCharsets.UTF_8.name().equalsIgnoreCase(documentEncoding)
				|| Charset.forName(documentEncoding) == StandardCharsets.UTF_8;
		}

		public String getXmlDeclaration(Serialization serialization, String documentEncoding) {
			try {
				StringBuilder sb = new StringBuilder();
				xmlDeclaration(serialization, documentEncoding, sb);
				return sb.toString();
			} catch(IOException e) {
				throw new AssertionError("IOException should not occur on StringBuilder", e);
			}
		}

		public Doctype xmlDeclaration(Serialization serialization, String documentEncoding, Appendable out) throws IOException {
			if(serialization == Serialization.XML && !isUTF8(documentEncoding)) {
				out.append("<?xml version=\"1.0\" encoding=\"");
				encodeTextInXhtmlAttribute(documentEncoding, out);
				out.append("\"?>\n");
			}
			return this;
		}

		/**
		 * Gets the <a href="https://www.w3schools.com/tags/tag_doctype.asp">HTML doctype declaration</a> line.
		 */
		public abstract String getDoctype(Serialization serialization);

		/**
		 * Appends the <a href="https://www.w3schools.com/tags/tag_doctype.asp">HTML doctype declaration</a> line, if any.
		 */
		public Doctype doctype(Serialization serialization, Appendable out) throws IOException {
			out.append(getDoctype(serialization));
			return this;
		}

		/**
		 * Gets the default script type/language attribute, if any.
		 */
		abstract public String getScriptType();

		/**
		 * Appends the default script type/language attribute, if any.
		 */
		public Doctype scriptType(Appendable out) throws IOException {
			out.append(getScriptType());
			return this;
		}

		/**
		 * Gets the default style type attribute, if any.
		 */
		abstract public String getStyleType();

		/**
		 * Appends the default style type attribute, if any.
		 */
		public Doctype styleType(Appendable out) throws IOException {
			out.append(getStyleType());
			return this;
		}

		/**
		 * Context init parameter that may be used to configure the default doctype within an application.
		 */
		public static final String DEFAULT_INIT_PARAM = Doctype.class.getName() + ".default";

		/**
		 * Determines the default doctype by first checking for {@linkplain ServletContext#getInitParameter(java.lang.String) context-param}
		 * of {@link #DEFAULT_INIT_PARAM}, the using {@link #HTML5} when unspecified or "default".
		 */
		public static Doctype getDefault(ServletContext servletContext) {
			String initParam = servletContext.getInitParameter(DEFAULT_INIT_PARAM);
			if(initParam != null) {
				initParam = initParam.trim();
				if(!initParam.isEmpty() && !"default".equalsIgnoreCase(initParam)) {
					return Doctype.valueOf(initParam.toUpperCase(Locale.ROOT));
				}
			}
			return HTML5;
		}

		private static final String REQUEST_ATTRIBUTE_NAME = Doctype.class.getName();

		/**
		 * Registers the doctype in effect for the request.
		 */
		public static void set(ServletRequest request, Doctype doctype) {
			request.setAttribute(REQUEST_ATTRIBUTE_NAME, doctype);
		}

		/**
		 * Replaces the doctype in effect for the request.
		 *
		 * @return  The previous attribute value, if any
		 */
		public static Doctype replace(ServletRequest request, Doctype doctype) {
			Doctype old = (Doctype)request.getAttribute(REQUEST_ATTRIBUTE_NAME);
			request.setAttribute(REQUEST_ATTRIBUTE_NAME, doctype);
			return old;
		}

		/**
		 * Gets the doctype in effect for the request, or {@linkplain #getDefault(javax.servlet.ServletContext) the default}
		 * when not yet {@linkplain #set(javax.servlet.ServletRequest, com.aoindustries.servlet.http.Html.Doctype) set}.
		 * <p>
		 * Once the default is resolved,
		 * {@linkplain #set(javax.servlet.ServletRequest, com.aoindustries.servlet.http.Html.Doctype) sets the request attribute}.
		 * </p>
		 */
		public static Doctype get(ServletContext servletContext, ServletRequest request) {
			Doctype doctype = (Doctype)request.getAttribute(REQUEST_ATTRIBUTE_NAME);
			if(doctype == null) {
				doctype = getDefault(servletContext);
				request.setAttribute(REQUEST_ATTRIBUTE_NAME, doctype);
			}
			return doctype;
		}
	}

	public static Html get(ServletContext servletContext, HttpServletRequest request, Writer out) {
		return new Html(
			Serialization.get(servletContext, request),
			Doctype.get(servletContext, request),
			out
		);
	}

	/**
	 * Unwraps the given chain writer.
	 */
	public static Html get(ServletContext servletContext, HttpServletRequest request, ChainWriter out) {
		return new Html(
			Serialization.get(servletContext, request),
			Doctype.get(servletContext, request),
			out.getPrintWriter()
		);
	}

	public final Serialization serialization;
	public final Doctype doctype;
	protected final Writer out;

	public Html(Serialization serialization, Doctype doctype, Writer out) {
		this.serialization = serialization;
		this.doctype = doctype;
		this.out = out;
	}

	/**
	 * @see Doctype#xmlDeclaration(com.aoindustries.servlet.http.Html.Serialization, java.lang.String, java.lang.Appendable)
	 */
	// TODO: Define here only since depends on both serialization and doctype
	public Html xmlDeclaration(String documentEncoding) throws IOException {
		doctype.xmlDeclaration(serialization, documentEncoding, out);
		return this;
	}

	/**
	 * @see Doctype#doctype(com.aoindustries.servlet.http.Html.Serialization, java.lang.Appendable)
	 */
	// TODO: Define here only since depends on both serialization and doctype
	public Html doctype() throws IOException {
		doctype.doctype(serialization, out);
		return this;
	}

	/**
	 * @see Serialization#selfClose(java.lang.Appendable)
	 */
	public Html selfClose() throws IOException {
		serialization.selfClose(out);
		return this;
	}

	public Html nl() throws IOException {
		out.write('\n');
		return this;
	}

	// TODO: Rename Html to Document, and make an element <html>?
	abstract public static class Element<E extends Element<E>> {

		protected final Html html;

		public Element(Html html) {
			this.html = html;
		}

		abstract protected E open() throws IOException;

		// TODO: Make shared attributeImpl methods, with things like a given value encoder and boolean if skip when null
		// TODO: Don't add attribute when value is null
		@SuppressWarnings("unchecked")
		public E attribute(String name, Object value) throws IOException {
			if(value instanceof AttributeWriter) return attribute(name, (AttributeWriter)value);
			if(value instanceof AttributeWriterE) {
				try {
					return (E)attributeE(name, (AttributeWriterE)value);
				} catch(Error|RuntimeException|IOException e) {
					throw e;
				} catch(Throwable t) {
					throw new WrappedException(t);
				}
			}
			// TODO: Validate attribute name?
			html.out.write(' ');
			html.out.write(name);
			if(value == null) {
				if(html.serialization == Serialization.XML) html.out.write("=\"\"");
			} else {
				html.out.write("=\"");
				Coercion.write(value, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return (E)this;
		}

		public MediaWriter attribute(String name) throws IOException {
			// TODO: Validate attribute name?
			html.out.write(' ');
			html.out.write(name);
			html.out.write("=\"");
			return new MediaWriter(textInXhtmlAttributeEncoder, html.out) {
				// Java 1.8: Lambda
				@Override
				public void close() throws IOException {
					html.out.write('"');
				}
			};
		}

		// Java 1.8: @Functional
		public static interface AttributeWriterE<Ex extends Throwable> {
			void writeAttribute(MediaWriter value) throws IOException, Ex;
		}

		@SuppressWarnings("unchecked")
		public <Ex extends Throwable> E attributeE(String name, AttributeWriterE<Ex> value) throws IOException, Ex {
			if(value == null) {
				return attribute(name, null);
			} else {
				try (MediaWriter out = attribute(name)) {
					value.writeAttribute(out);
				}
			}
			return (E)this;
		}

		// Java 1.8: @Functional
		public static interface AttributeWriter extends AttributeWriterE<RuntimeException> {
			@Override
			void writeAttribute(MediaWriter value) throws IOException;
		}

		public E attribute(String name, AttributeWriter value) throws IOException {
			return attributeE(name, value);
		}

		// TODO: Auto-closeable attribute writers for streaming implementations

		// <editor-fold desc="Global Attributes">
		@SuppressWarnings("unchecked")
		public E clazz(Object clazz) throws IOException {
			if(clazz != null) {
				html.out.write(" class=\"");
				Coercion.write(clazz, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return (E)this;
		}

		@SuppressWarnings("unchecked")
		public E id(Object id) throws IOException {
			if(id != null) {
				html.out.write(" id=\"");
				Coercion.write(id, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return (E)this;
		}

		@SuppressWarnings("unchecked")
		public E style(Object style) throws IOException {
			if(style != null) {
				html.out.write(" style=\"");
				Coercion.write(style, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return (E)this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_global_tabindex.asp">HTML Global tabindex Attribute</a>.
		 */
		@SuppressWarnings("unchecked")
		public E tabindex(Integer tabindex) throws IOException {
			if(html.doctype != Doctype.HTML5) {
				throw new LocalizedIllegalArgumentException(
					accessor,
					"Html.Element.invalidGlobalAttributeForDoctype",
					html.doctype,
					"tabindex"
				);
			}
			if(tabindex != null) {
				html.out.write(" tabindex=\"");
				html.out.write(tabindex.toString());
				html.out.write('"');
			}
			return (E)this;
		}

		@SuppressWarnings("unchecked")
		public E title(Object title) throws IOException {
			if(title != null) {
				html.out.write(" title=\"");
				// Allow text markup from translations
				Coercion.write(title, MarkupType.TEXT, textInXhtmlAttributeEncoder, false, html.out);
				html.out.write('"');
			}
			return (E)this;
		}
		// </editor-fold>

		// <editor-fold desc="Global Event Attributes">
		// https://www.w3schools.com/tags/ref_eventattributes.asp

		// <editor-fold desc="Mouse Events">
		/**
		 * See <a href="https://www.w3schools.com/tags/ev_onclick.asp">HTML onclick Event Attribute</a>.
		 */
		@SuppressWarnings("unchecked")
		public E onclick(Object onclick) throws IOException {
			if(onclick != null) {
				if(onclick instanceof AttributeWriter) return onclick((AttributeWriter)onclick);
				if(onclick instanceof AttributeWriterE) {
					try {
						return onclickE((AttributeWriterE<?>)onclick);
					} catch(Error|RuntimeException|IOException e) {
						throw e;
					} catch(Throwable t) {
						throw new WrappedException(t);
					}
				}
				html.out.write(" onclick=\"");
				// TODO: Find more places where we can do javascript markups (ao-taglib...)
				Coercion.write(onclick, MarkupType.JAVASCRIPT, javaScriptInXhtmlAttributeEncoder, false, html.out);
				html.out.write('"');
			}
			return (E)this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/ev_onclick.asp">HTML onclick Event Attribute</a>.
		 */
		public MediaWriter onclick() throws IOException {
			html.out.write(" onclick=\"");
			return new MediaWriter(javaScriptInXhtmlAttributeEncoder, html.out) {
				// Java 1.8: Lambda
				@Override
				public void close() throws IOException {
					html.out.write('"');
				}
			};
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/ev_onclick.asp">HTML onclick Event Attribute</a>.
		 */
		@SuppressWarnings("unchecked")
		public <Ex extends Throwable> E onclickE(AttributeWriterE<Ex> onclick) throws IOException, Ex {
			if(onclick != null) {
				try (MediaWriter out = onclick()) {
					onclick.writeAttribute(out);
				}
			}
			return (E)this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/ev_onclick.asp">HTML onclick Event Attribute</a>.
		 */
		public E onclick(AttributeWriter onclick) throws IOException {
			return onclickE(onclick);
		}

		// </editor-fold>

		// </editor-fold>
	}

	abstract public static class EmptyElement<E extends EmptyElement<E>> extends Element<E> {

		public EmptyElement(Html html) {
			super(html);
		}

		/**
		 * Closes this element.
		 */
		public Html __() throws IOException {
			html.selfClose();
			return html;
		}
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_br.asp">HTML br tag</a>.
	 */
	public static class Br extends EmptyElement<Br> {

		public Br(Html html) {
			super(html);
		}

		@Override
		protected Br open() throws IOException {
			html.out.write("<br");
			return this;
		}
	}

	protected Br br;

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_br.asp">HTML br tag</a>.
	 */
	public Br br() throws IOException {
		if(br == null) br = new Br(this);
		return br.open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_br.asp">HTML br tag</a>.
	 */
	public Html br__() throws IOException {
		return br().__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_hr.asp">HTML hr tag</a>.
	 */
	public static class Hr extends EmptyElement<Hr> {

		public Hr(Html html) {
			super(html);
		}

		@Override
		protected Hr open() throws IOException {
			html.out.write("<hr");
			return this;
		}
	}

	protected Hr hr;

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_hr.asp">HTML hr tag</a>.
	 */
	public Hr hr() throws IOException {
		if(hr == null) hr = new Hr(this);
		return hr.open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_hr.asp">HTML hr tag</a>.
	 */
	public Html hr__() throws IOException {
		return hr().__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 */
	public static class Input extends EmptyElement<Input> {

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
		 */
		public enum Type {
			BUTTON("button") {
				@Override
				public MarkupType getMarkupType() {
					return MarkupType.TEXT;
				}
			},
			CHECKBOX("checkbox"),
			COLOR("color"),
			DATE("date"),
			DATETIME_LOCAL("datetime-local"),
			EMAIL("email"),
			FILE("file"),
			HIDDEN("hidden"),
			IMAGE("image"),
			MONTH("month"),
			NUMBER("number"),
			PASSWORD("password"),
			RADIO("radio"),
			RANGE("range"),
			RESET("reset") {
				@Override
				public MarkupType getMarkupType() {
					return MarkupType.TEXT;
				}
			},
			SEARCH("search"),
			SUBMIT("submit") {
				@Override
				public MarkupType getMarkupType() {
					return MarkupType.TEXT;
				}
			},
			TEL("tel"),
			TEXT("text"),
			TIME("time"),
			URL("url"),
			WEEK("week");

			private final String value;
			private final Doctype requiredDoctype;

			private Type(String value, Doctype requiredDoctype) {
				this.value = value;
				this.requiredDoctype = requiredDoctype;
			}

			private Type(String value) {
				this(value, null);
			}

			@Override
			public String toString() {
				return value;
			}

			public Doctype getRequiredDoctype() {
				return requiredDoctype;
			}

			/**
			 * Gets the interactive editor markup type or {@code null} to not alter
			 * the value.
			 */
			public MarkupType getMarkupType() {
				return null;
			}

			private static final Type[] values = values();
			private static final Map<String,Type> byLowerValue = new HashMap<>(values.length*4/3+1);
			static {
				for(Type type : values) {
					byLowerValue.put(type.value.toLowerCase(Locale.ROOT), type);
				}
			}
			public static Type valueOfWithLower(String name) {
				Type type = byLowerValue.get(name.toLowerCase(Locale.ROOT));
				if(type == null) {
					type = valueOf(name.toUpperCase(Locale.ROOT));
				}
				return type;
			}
		}

		private final Type type;

		public Input(Html html, Type type) {
			super(html);
			this.type = type;
		}

		public Input(Html html, String type) {
			this(html, (type == null) ? null : Type.valueOfWithLower(type));
		}

		public Input(Html html) {
			this(html, (Type)null);
		}

		@Override
		protected Input open() throws IOException {
			html.out.write("<input");
			Input i = type(type);
			assert i == this;
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_checked.asp">HTML input checked Attribute</a>.
		 */
		public Input checked(boolean checked) throws IOException {
			if(checked) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(" checked");
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(" checked=\"checked\"");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_disabled.asp">HTML input disabled Attribute</a>.
		 */
		public Input disabled(boolean disabled) throws IOException {
			if(disabled) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(" disabled");
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(" disabled=\"disabled\"");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_maxlength.asp">HTML input maxlength Attribute</a>.
		 */
		public Input maxlength(Integer maxlength) throws IOException {
			if(maxlength != null) {
				html.out.write(" maxlength=\"");
				html.out.write(maxlength.toString());
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_name.asp">HTML input name Attribute</a>.
		 */
		public Input name(Object name) throws IOException {
			if(name != null) {
				html.out.write(" name=\"");
				Coercion.write(name, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_readonly.asp">HTML input readonly Attribute</a>.
		 */
		public Input readonly(boolean readonly) throws IOException {
			if(readonly) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(" readonly");
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(" readonly=\"readonly\"");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_size.asp">HTML input size Attribute</a>.
		 */
		public Input size(Integer size) throws IOException {
			if(size != null) {
				html.out.write(" size=\"");
				html.out.write(size.toString());
				html.out.write('"');
			}
			return this;
		}

		/**
		 * {@inheritDoc}
		 * When in input element, valid in all doctypes.
		 */
		@Override
		public Input tabindex(Integer tabindex) throws IOException {
			if(tabindex != null) {
				html.out.write(" tabindex=\"");
				html.out.write(tabindex.toString());
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
		 *
		 * @return  {@code this} when type unchanged, or an instance of {@link Input} for the given type.
		 */
		public Input type(Type type) throws IOException {
			if(type != null) {
				Doctype requiredDoctype = type.getRequiredDoctype();
				if(requiredDoctype != null && html.doctype != requiredDoctype) {
					throw new LocalizedIllegalArgumentException(
						accessor,
						"Html.Input.typeRequiresDoctype",
						type.value,
						requiredDoctype,
						html.doctype
					);
				}
				html.out.write(" type=\"");
				html.out.write(type.value);
				html.out.write('"');
			}
			return (type == this.type) ? this : html.getInput(type);
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
		 *
		 * @return  {@code this} when type unchanged, or an instance of {@link Input} for the given type.
		 */
		public Input type(String type) throws IOException {
			if(type != null) {
				type(Type.valueOfWithLower(type));
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_input_value.asp">HTML input value Attribute</a>.
		 */
		public Input value(Object value) throws IOException {
			if(value != null) {
				html.out.write(" value=\"");
				// Allow text markup from translations
				Coercion.write(
					value,
					(type == null) ? null : type.getMarkupType(),
					textInXhtmlAttributeEncoder,
					false,
					html.out
				);
				html.out.write('"');
			}
			return this;
		}
	}

	private Input input;
	private EnumMap<Input.Type,Input> inputs;

	protected Input getInput(Input.Type type) {
		if(type == null) {
			if(input == null) input = new Input(this);
			return input;
		} else {
			Input i;
			if(inputs == null) {
				inputs = new EnumMap<>(Input.Type.class);
				i = null;
			} else {
				i = inputs.get(type);
			}
			if(i == null) {
				i = new Input(this, type);
				inputs.put(type, i);
			}
			return i;
		}
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
	 */
	public Input input(Input.Type type) throws IOException {
		return getInput(type).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
	 */
	public Html input__(Input.Type type) throws IOException {
		return input(type).__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
	 */
	public Input input(String type) throws IOException {
		return input((type == null) ? (Input.Type)null : Input.Type.valueOfWithLower(type));
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_input_type.asp">HTML input type Attribute</a>.
	 */
	public Html input__(String type) throws IOException {
		return input(type).__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 */
	public Input input() throws IOException {
		return input((Input.Type)null);
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_input.asp">HTML input tag</a>.
	 */
	public Html input__() throws IOException {
		return input().__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_link.asp">HTML link tag</a>.
	 */
	public static class Link extends EmptyElement<Link> {

		public Link(Html html) {
			super(html);
		}

		@Override
		protected Link open() throws IOException {
			html.out.write("<link");
			return this;
		}

		/**
		 * See <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/CORS_settings_attributes">The crossorigin attribute: Requesting CORS access to content</a>.
		 */
		public enum Crossorigin {
			ANONYMOUS(
				" crossorigin",
				" crossorigin=\"anonymous\""
			),
			USE_CREDENTIALS(
				" crossorigin=\"use-credentials\"",
				" crossorigin=\"use-credentials\""
			);
			private final String sgml;
			private final String xml;
			private Crossorigin(String sgml, String xml) {
				this.sgml = sgml;
				this.xml = xml;
			}
		}

		/**
		 * See <a href="https://developer.mozilla.org/en-US/docs/Web/HTML/CORS_settings_attributes">The crossorigin attribute: Requesting CORS access to content</a>.
		 */
		public Link crossorigin(Crossorigin crossorigin) throws IOException {
			if(crossorigin != null) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(crossorigin.sgml);
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(crossorigin.xml);
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_link_href.asp">HTML link href Attribute</a>.
		 */
		public Link href(Object href) throws IOException {
			if(href != null) {
				html.out.write(" href=\"");
				// TODO: UrlInXhtmlAttributeEncoder once RFC 3987 supported
				Coercion.write(href, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * <a href="https://html.spec.whatwg.org/multipage/semantics.html#the-link-element">HTML Standard</a>:
		 * <blockquote>
		 *   A link element must have either a rel attribute or an itemprop attribute, but not both.
		 * </blockquote>
		 */
		private Object itemprop;

		// TODO: Is global property, move there and add See comment, still checking for link-specific rules here
		public Link itemprop(Object itemprop) throws IOException {
			itemprop = Coercion.trimNullIfEmpty(itemprop);
			if(itemprop != null) {
				if(this.itemprop != null) {
					throw new LocalizedIllegalStateException(
						accessor,
						"Html.duplicateAttribute",
						"link",
						"itemprop",
						Coercion.toString(this.itemprop),
						Coercion.toString(itemprop)
					);
				}
				this.itemprop = itemprop;
				if(this.rel != null) {
					throw new LocalizedIllegalStateException(
						accessor,
						"Html.Item.relOrItemprop"
					);
				}
				html.out.write(" itemprop=\"");
				Coercion.write(itemprop, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_link_media.asp">HTML link media Attribute</a>.
		 */
		public Link media(Object media) throws IOException {
			media = Coercion.trimNullIfEmpty(media);
			if(media != null) {
				html.out.write(" media=\"");
				Coercion.write(media, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://html.spec.whatwg.org/multipage/semantics.html#attr-link-rel">HTML Standard</a>.
		 * See <a href="https://www.w3schools.com/tags/att_link_rel.asp">HTML link rel Attribute</a>.
		 */
		public enum Rel {
			ALTERNATE("alternate"),
			AUTHOR("author"), // w3schools only
			CANONICAL("canonical"), // TODO: This is not in the last.  Should we support arbitrary String values, like Script.type?
			DNS_PREFETCH("dns-prefetch"),
			HELP("help"), // w3schools only
			ICON("icon"),
			LICENSE("license"), // w3schools only
			MODULEPRELOAD("modulepreload"), // HTML Standard only
			NEXT("next"),
			PINGBACK("pingback"),
			PRECONNECT("preconnect"),
			PREFETCH("prefetch"),
			PRELOAD("preload"),
			PRERENDER("prerender"),
			PREV("prev"), // w3schools only
			SEARCH("search"),
			STYLESHEET("stylesheet");

			private final String value;
			// TODO: Verify values by doctype

			private Rel(String value) {
				this.value = value;
			}

			@Override
			public String toString() {
				return value;
			}
		}

		private Object rel;

		/**
		 * <a href="https://html.spec.whatwg.org/multipage/semantics.html#the-link-element">HTML Standard</a>:
		 * <blockquote>
		 *   A link element must have either a rel attribute or an itemprop attribute, but not both.
		 * </blockquote>
		 *
		 * See <a href="https://html.spec.whatwg.org/multipage/semantics.html#attr-link-rel">HTML Standard</a>.
		 * See <a href="https://www.w3schools.com/tags/att_link_rel.asp">HTML link rel Attribute</a>.
		 */
		public Link rel(Object rel) throws IOException {
			rel = Coercion.trimNullIfEmpty(rel);
			if(rel != null) {
				if(this.rel != null) {
					throw new LocalizedIllegalStateException(
						accessor,
						"Html.duplicateAttribute",
						"link",
						"rel",
						Coercion.toString(this.rel),
						Coercion.toString(rel)
					);
				}
				this.rel = rel;
				if(this.itemprop != null) {
					throw new LocalizedIllegalStateException(
						accessor,
						"Html.Item.relOrItemprop"
					);
				}
				html.out.write(" rel=\"");
				Coercion.write(rel, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * <a href="https://html.spec.whatwg.org/multipage/semantics.html#the-link-element">HTML Standard</a>:
		 * <blockquote>
		 *   A link element must have either a rel attribute or an itemprop attribute, but not both.
		 * </blockquote>
		 *
		 * See <a href="https://html.spec.whatwg.org/multipage/semantics.html#attr-link-rel">HTML Standard</a>.
		 * See <a href="https://www.w3schools.com/tags/att_link_rel.asp">HTML link rel Attribute</a>.
		 */
		public Link rel(Rel rel) throws IOException {
			return rel((rel == null) ? (Object)null : rel.toString());
		}

		private String type;

		/**
		 * If the rel is {@link Rel#STYLESHEET}, the type is {@link #DEFAULT_TYPE},
		 * and the {@link Doctype} is {@link Doctype#HTML5}, skips writing
		 * the type.
		 *
		 * See <a href="https://www.w3schools.com/tags/att_link_type.asp">HTML link type Attribute</a>.
		 */
		public Link type(String type) throws IOException {
			// TODO: type = trimNullIfEmpty(type);
			this.type = type;
			if(
				type != null
				&& !(
					html.doctype == Doctype.HTML5
					&& rel != null
					&& rel.toString().equals(Rel.STYLESHEET.toString())
					&& Style.Type.TEXT_CSS.getContentType().equalsIgnoreCase(type)
				)
			) {
				html.out.write(" type=\"");
				encodeTextInXhtmlAttribute(type, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * If the rel is {@link Rel#STYLESHEET}, a {@linkplain #type(java.lang.String) type}
		 * has not been written, and the {@link Doctype} is not {@link Doctype#HTML5},
		 * writes the default type {@link #DEFAULT_TYPE} for backward compatibility.
		 * <p>
		 * <a href="https://html.spec.whatwg.org/multipage/semantics.html#the-link-element">HTML Standard</a>:
		 * </p>
		 * <blockquote>
		 *   A link element must have either a rel attribute or an itemprop attribute, but not both.
		 * </blockquote>
		 */
		@Override
		public Html __() throws IOException {
			if(
				type == null
				&& html.doctype != Doctype.HTML5
				&& rel != null
				&& rel.toString().equals(Rel.STYLESHEET.toString())
			) {
				html.out.write(" type=\"");
				html.out.write(Style.Type.TEXT_CSS.getContentType());
				html.out.write('"');
			}
			super.__();
			if(rel == null && itemprop == null) {
				throw new LocalizedIllegalStateException(
					accessor,
					"Html.Item.relOrItemprop"
				);
			}
			return html;
		}
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_link.asp">HTML link tag</a>.
	 */
	public Link link() throws IOException {
		return new Link(this).open();
	}

	// No link__(), since either rel or itemprop is required

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_option.asp">HTML option tag</a>.
	 */
	public static class Option extends Element<Option> {

		public Option(Html html) {
			super(html);
		}

		@Override
		protected Option open() throws IOException {
			html.out.write("<option");
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_option_disabled.asp">HTML option disabled Attribute</a>.
		 */
		public Option disabled(boolean disabled) throws IOException {
			if(disabled) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(" disabled");
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(" disabled=\"disabled\"");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_option_label.asp">HTML option label Attribute</a>.
		 *
		 * @deprecated  Although still part of the HTML specification, there is a
		 *              <a href="https://bugzilla.mozilla.org/show_bug.cgi?id=40545">20-year old Firefox bug</a>
		 *              that the label attribute is not supported.  We are deprecating
		 *              this method to make it clear it should probably not be used, as the
		 *              effect of label can be attained through the value attribute and
		 *              tag body anyway.
		 */
		@Deprecated
		public Option label(Object label) throws IOException {
			if(label != null) {
				html.out.write(" label=\"");
				Coercion.write(label, MarkupType.TEXT, textInXhtmlAttributeEncoder, false, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_option_selected.asp">HTML option selected Attribute</a>.
		 */
		public Option selected(boolean selected) throws IOException {
			if(selected) {
				if(html.serialization == Html.Serialization.SGML) {
					html.out.write(" selected");
				} else {
					assert html.serialization == Serialization.XML;
					html.out.write(" selected=\"selected\"");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_option_value.asp">HTML option value Attribute</a>.
		 */
		public Option value(Object value) throws IOException {
			if(value != null) {
				html.out.write(" value=\"");
				Coercion.write(value, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * Writes the text body and closes the tag.
		 */
		public Html innerText(Object text) throws IOException {
			html.out.write('>');
			// TODO: Only allow markup when the value has been set (auto-set value from text like ao-taglib?)
			// Allow text markup from translations
			Coercion.write(text, MarkupType.TEXT, textInXhtmlEncoder, false, html.out);
			html.out.write("</option>");
			return html;
		}

		/**
		 * Performs URL rewriting via the given {@link EncodingContext}.
		 */
		// TODO: indent variant, indent all lines in a filter?
		public MediaWriter innerText(EncodingContext context) throws IOException {
			try {
				html.out.write('>');
				return new MediaWriter(
					MediaEncoder.getInstance(
						context,
						MediaType.TEXT,
						MediaType.XHTML
					),
					html.out
				) {
					@Override
					public void close() throws IOException {
						html.out.write("</option>");
					}
				};
			} catch(MediaException e) {
				throw new IOException(e);
			}
		}

		/**
		 * Performs URL rewriting via {@link HttpServletResponseEncodingContext}.
		 */
		public MediaWriter innerText(HttpServletResponse response) throws IOException {
			return innerText(new HttpServletResponseEncodingContext(response));
		}

		/**
		 * Does not perform any URL rewriting.
		 */
		public MediaWriter innerText() throws IOException {
			return innerText(
				// Java 1.8: Lambda
				new EncodingContext() {
					@Override
					public String encodeURL(String url) {
						return url;
					}
				}
			);
		}

		/**
		 * Closes to form an empty option.
		 */
		public Html __() throws IOException {
			html.out.write("></option>");
			return html;
		}
	}

	protected Option option;

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_option.asp">HTML option tag</a>.
	 */
	public Option option() throws IOException {
		if(option == null) option = new Option(this);
		return option.open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_option.asp">HTML option tag</a>.
	 */
	public Html option__() throws IOException {
		return option().__();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_script.asp">HTML script tag</a>.
	 */
	public static class Script extends Element<Script> {

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_type.asp">HTML script type Attribute</a>.
		 */
		public enum Type {
			/**
			 * The default type for (X)HTML 5.
			 */
			APPLICATION_JAVASCRIPT("application/javascript"),

			/**
			 * The default type for XHTML 1.0 / HTML 4.
			 */
			TEXT_JAVASCRIPT("text/javascript"),

			/**
			 * A JSON script.
			 */
			APPLICATION_JSON("application/json"),

			/**
			 * A JSON linked data script.
			 */
			APPLICATION_JD_JSON("application/ld+json"),

			APPLICATION_ECMASCRIPT("application/ecmascript");

			private final String contentType;

			private Type(String contentType) {
				this.contentType = contentType;
			}

			@Override
			public String toString() {
				return contentType;
			}

			public String getContentType() {
				return contentType;
			}

			private static boolean assertAllLowerCase() {
				for(Type type : values()) {
					if(!type.contentType.equals(type.contentType.toLowerCase(Locale.ROOT))) throw new AssertionError("Content types must be lowercase as looked-up later");
				}
				return true;
			}
			static {
				assert assertAllLowerCase();
			}
		}

		private final String type;

		public Script(Html html) {
			super(html);
			this.type = null;
		}

		public Script(Html html, String type) {
			super(html);
			this.type = (type == null) ? null : type.toLowerCase(Locale.ROOT);
		}

		public Script(Html html, Type type) {
			super(html);
			this.type = (type == null) ? null : type.getContentType();
		}

		@Override
		protected Script open() throws IOException {
			html.out.write("<script");
			return type();
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_async.asp">HTML script async Attribute</a>.
		 */
		public Script async(boolean async) throws IOException {
			if(async) {
				if(html.serialization == Html.Serialization.XML) {
					html.out.write(" async=\"async\"");
				} else {
					html.out.write(" async");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_defer.asp">HTML script defer Attribute</a>.
		 */
		public Script defer(boolean defer) throws IOException {
			if(defer) {
				if(html.serialization == Html.Serialization.XML) {
					html.out.write(" defer=\"defer\"");
				} else {
					html.out.write(" defer");
				}
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_src.asp">HTML script src Attribute</a>.
		 */
		public Script src(Object src) throws IOException {
			if(src != null) {
				html.out.write(" src=\"");
				// TODO: UrlInXhtmlAttributeEncoder once RFC 3987 supported
				Coercion.write(src, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_type.asp">HTML script type Attribute</a>.
		 *
		 * @see Doctype#scriptType(java.lang.Appendable)
		 */
		protected Script type() throws IOException {
			if(
				type == null
				|| type.equals(Type.APPLICATION_JAVASCRIPT.getContentType())
				|| type.equals(Type.TEXT_JAVASCRIPT.getContentType())
			) {
				html.doctype.scriptType(html.out);
			} else {
				html.out.write(" type=\"");
				encodeTextInXhtmlAttribute(type, html.out);
				html.out.write('"');
			}
			return this;
		}

		protected MediaType getMediaType() throws IOException {
			try {
				return type == null ? MediaType.JAVASCRIPT : MediaType.getMediaTypeForContentType(type);
			} catch(MediaException e) {
				throw new IOException(e);
			}
		}

		protected MediaEncoder getMediaEncoder(MediaType mediaType) throws IOException {
			try {
				return MediaEncoder.getInstance(null, mediaType, MediaType.XHTML);
			} catch(MediaException e) {
				throw new IOException(e);
			}
		}

		private boolean didBody;

		protected void startBody() throws IOException {
			if(!didBody) {
				html.out.write(">\n");
				cdata.start();
				didBody = true;
			}
		}

		public Script out(Object script) throws IOException {
			if(script != null) {
				if(script instanceof ScriptWriter) return out((ScriptWriter)script);
				if(script instanceof ScriptWriterE) {
					try {
						return outE((ScriptWriterE<?>)script);
					} catch(Error|RuntimeException|IOException e) {
						throw e;
					} catch(Throwable t) {
						throw new WrappedException(t);
					}
				}
				MediaType mediaType = getMediaType();
				MediaEncoder encoder = getMediaEncoder(mediaType);
				startBody();
				// Allow text markup from translations
				Coercion.write(script, mediaType.getMarkupType(), encoder, false, html.out);
			}
			return this;
		}

		/**
		 * Writes the script, automatically closing the script via
		 * {@link #__()} on {@link MediaWriter#close()}.  This is well suited
		 * for use in a try-with-resources block.
		 */
		public MediaWriter out() throws IOException {
			MediaEncoder encoder = getMediaEncoder(getMediaType());
			startBody();
			return new MediaWriter(encoder, html.out) {
				@Override
				public void close() throws IOException {
					__();
				}
			};
		}

		// Java 1.8: @Functional
		public static interface ScriptWriterE<Ex extends Throwable> {
			void writeScript(MediaWriter script) throws IOException, Ex;
		}

		public <Ex extends Throwable> Script outE(ScriptWriterE<Ex> script) throws IOException, Ex {
			if(script != null) {
				MediaEncoder encoder = getMediaEncoder(getMediaType());
				startBody();
				script.writeScript(
					new MediaWriter(
						encoder,
						new NoCloseWriter(html.out)
					)
				);
			}
			return this;
		}

		// Java 1.8: @Functional
		public static interface ScriptWriter extends ScriptWriterE<RuntimeException> {
			@Override
			void writeScript(MediaWriter script) throws IOException;
		}

		public Script out(ScriptWriter script) throws IOException {
			return outE(script);
		}

		// TODO: Hide cdata?
		public class Cdata {
			public Script start(String indent) throws IOException {
				if(
					html.serialization == Html.Serialization.XML
					&& (
						Type.APPLICATION_JAVASCRIPT.getContentType().equals(type)
						|| Type.TEXT_JAVASCRIPT.getContentType().equals(type)
					)
				) {
					if(indent != null) html.out.write(indent);
					html.out.write("// <![CDATA[\n");
				}
				return Script.this;
			}
			public Script start() throws IOException {
				return start(null);
			}
			public Script end(String indent) throws IOException {
				if(
					html.serialization == Html.Serialization.XML
					&& (
						Type.APPLICATION_JAVASCRIPT.getContentType().equals(type)
						|| Type.TEXT_JAVASCRIPT.getContentType().equals(type)
					)
				) {
					if(indent != null) html.out.write(indent);
					html.out.write("// ]]>\n");
				}
				return Script.this;
			}
			public Script end() throws IOException {
				return end(null);
			}
		}
		public final Cdata cdata = new Cdata();

		public Html __() throws IOException {
			if(!didBody) {
				html.out.write("></script>");
			} else {
				// TODO: Track what was written and avoid unnecessary newline?
				html.nl();
				cdata.end();
				html.out.write("</script>");
			}
			return html;
		}
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_script.asp">HTML script tag</a>.
	 *
	 * @see Doctype#scriptType(java.lang.Appendable)
	 */
	public Script script() throws IOException {
		return new Script(this).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_script.asp">HTML script tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_script_type.asp">HTML script type Attribute</a>.
	 */
	public Script script(String type) throws IOException {
		return new Script(this, type).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_script.asp">HTML script tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_script_type.asp">HTML script type Attribute</a>.
	 */
	public Script script(Script.Type type) throws IOException {
		return new Script(this, type).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_style.asp">HTML style tag</a>.
	 */
	public static class Style extends Element<Style> {

		/**
		 * See <a href="https://www.w3schools.com/tags/att_script_type.asp">HTML script type Attribute</a>.
		 */
		public enum Type {
			/**
			 * The default type.
			 */
			TEXT_CSS("text/css");

			private final String contentType;

			private Type(String contentType) {
				this.contentType = contentType;
			}

			@Override
			public String toString() {
				return contentType;
			}

			public String getContentType() {
				return contentType;
			}

			private static boolean assertAllLowerCase() {
				for(Type type : values()) {
					if(!type.contentType.equals(type.contentType.toLowerCase(Locale.ROOT))) throw new AssertionError("Content types must be lowercase as looked-up later");
				}
				return true;
			}
			static {
				assert assertAllLowerCase();
			}
		}

		private final String type;

		public Style(Html html) {
			super(html);
			this.type = null;
		}

		public Style(Html html, String type) {
			super(html);
			this.type = (type == null) ? null : type.toLowerCase(Locale.ROOT);
		}

		public Style(Html html, Type type) {
			super(html);
			this.type = (type == null) ? null : type.getContentType();
		}

		@Override
		protected Style open() throws IOException {
			html.out.write("<style");
			return type();
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_style_media.asp">HTML style media Attribute</a>.
		 */
		public Style media(Object media) throws IOException {
			media = Coercion.trimNullIfEmpty(media); // TODO: Review which attributes should be trimmed
			if(media != null) {
				html.out.write(" media=\"");
				Coercion.write(media, textInXhtmlAttributeEncoder, html.out);
				html.out.write('"');
			}
			return this;
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/att_style_type.asp">HTML style type Attribute</a>.
		 *
		 * @see Doctype#styleType(java.lang.Appendable)
		 */
		protected Style type() throws IOException {
			if(
				type == null
				|| type.equals(Type.TEXT_CSS.getContentType())
			) {
				html.doctype.styleType(html.out);
			} else {
				html.out.write(" type=\"");
				encodeTextInXhtmlAttribute(type, html.out);
				html.out.write('"');
			}
			return this;
		}

		protected MediaType getMediaType() throws IOException {
			return MediaType.TEXT; // TODO: Version for CSS (with automatic URL rewriting?)
		}

		protected MediaEncoder getMediaEncoder(MediaType mediaType) throws IOException {
			// TODO: This is in a CDATA context, is this the correct way?  Probably not, but how to protect close CDATA ]]>?
			return textInXhtmlEncoder;
		}

		private boolean didBody;

		protected void startBody() throws IOException {
			if(!didBody) {
				html.out.write(">\n");
				cdata.start();
				didBody = true;
			}
		}

		// TODO: Out parameter with MediaType, that automatically picks the encoder
		// TODO: Separate "Write" for direct writing (no encoding)?
		// TODO: FUnctional versions for Java 1.8
		public Style out(Object style) throws IOException {
			if(style != null) {
				if(style instanceof StyleWriter) return out((StyleWriter)style);
				if(style instanceof StyleWriterE) {
					try {
						return outE((StyleWriterE<?>)style);
					} catch(Error|RuntimeException|IOException e) {
						throw e;
					} catch(Throwable t) {
						throw new WrappedException(t);
					}
				}
				MediaEncoder encoder = getMediaEncoder(getMediaType());
				startBody();
				// Allow text markup from translations
				Coercion.write(
					style,
					// TODO: Compatible, but better to make an explicit value for MarkupType.CSS: mediaType.getMarkupType()
					MarkupType.JAVASCRIPT,
					encoder,
					false,
					html.out
				);
			}
			return this;
		}

		/**
		 * Writes the style, automatically closing the style via
		 * {@link #__()} on {@link MediaWriter#close()}.  This is well suited
		 * for use in a try-with-resources block.
		 */
		public MediaWriter out() throws IOException {
			MediaEncoder encoder = getMediaEncoder(getMediaType());
			startBody();
			return new MediaWriter(encoder, html.out) {
				@Override
				public void close() throws IOException {
					__();
				}
			};
		}

		// Java 1.8: @Functional
		public static interface StyleWriterE<Ex extends Throwable> {
			void writeStyle(MediaWriter style) throws IOException, Ex;
		}

		public <Ex extends Throwable> Style outE(StyleWriterE<Ex> style) throws IOException, Ex {
			if(style != null) {
				MediaEncoder encoder = getMediaEncoder(getMediaType());
				startBody();
				style.writeStyle(
					new MediaWriter(
						encoder,
						new NoCloseWriter(html.out)
					)
				);
			}
			return this;
		}

		// Java 1.8: @Functional
		public static interface StyleWriter extends StyleWriterE<RuntimeException> {
			@Override
			void writeStyle(MediaWriter style) throws IOException;
		}

		public Style out(StyleWriter style) throws IOException {
			return outE(style);
		}

		// TODO: Hide cdata?
		public class Cdata {
			public Style start(String indent) throws IOException {
				if(html.serialization == Html.Serialization.XML) {
					if(indent != null) html.out.write(indent);
					html.out.write("/* <![CDATA[ */\n");
				}
				return Style.this;
			}
			public Style start() throws IOException {
				return start(null);
			}
			public Style end(String indent) throws IOException {
				if(html.serialization == Html.Serialization.XML) {
					if(indent != null) html.out.write(indent);
					html.out.write("/* ]]> */\n");
				}
				return Style.this;
			}
			public Style end() throws IOException {
				return end(null);
			}
		}
		public final Cdata cdata = new Cdata();

		public Html __() throws IOException {
			if(!didBody) {
				html.out.write("></style>");
			} else {
				// TODO: Track what was written and avoid unnecessary newline?
				html.nl();
				cdata.end();
				html.out.write("</style>");
			}
			return html;
		}
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_style.asp">HTML style tag</a>.
	 *
	 * @see Doctype#styleType(java.lang.Appendable)
	 */
	public Style style() throws IOException {
		return new Style(this).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_style.asp">HTML style tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_style_type.asp">HTML style type Attribute</a>.
	 */
	public Style style(String type) throws IOException {
		return new Style(this, type).open();
	}

	/**
	 * See <a href="https://www.w3schools.com/tags/tag_style.asp">HTML style tag</a>.
	 * See <a href="https://www.w3schools.com/tags/att_style_type.asp">HTML style type Attribute</a>.
	 */
	public Style style(Style.Type type) throws IOException {
		return new Style(this, type).open();
	}

	// TODO: style__() - go directly to out, since no attributes? Lambda versions, too

	// TODO: A version called HtmlWriter that extends ChainWriter to avoid all this passing of appendables?
	// TODO: html.input.style.type().print("...").__().  How far do we take this?
}
