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

import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.servlet.ServletUtil;
import com.aoindustries.util.StringUtility;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Enumeration;
import java.util.List;
import javax.servlet.ServletContext;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

/**
 * Static utilities that may be useful for HTML/XHTML in servlet/JSP/taglib environments.
 *
 * @author  AO Industries, Inc.
 *
 * @see ServletUtil
 */
public class Html {

	private Html() {
	}

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
		HTML {
			@Override
			public String getContentType() {
				return CONTENT_TYPE_HTML;
			}
		},
		XHTML {
			@Override
			public String getContentType() {
				return CONTENT_TYPE_XHTML;
			}
		};

		/**
		 * Gets the content-type header to use for this serialization.
		 */
		abstract public String getContentType();

		private static boolean startsWithIgnoreCase(String value, String prefix) {
			int prefixLen = prefix.length();
			return
				value.length() >= prefixLen
				&& value.regionMatches(
					true,
					0,
					prefix,
					0,
					prefixLen
				);
		}

		/**
		 * Gets the serialization represented by the given content type.
		 *
		 * @throws IllegalArgumentException when the value is neither {@link #CONTENT_TYPE_HTML} or
		 *                                  {@link #CONTENT_TYPE_XHTML}, case-insensitive, or prefixed
		 *                                  with same followed by ";"
		 */
		public static Serialization get(String contentType) throws IllegalArgumentException {
			if(CONTENT_TYPE_HTML.equalsIgnoreCase(contentType)) return HTML;
			if(CONTENT_TYPE_XHTML.equalsIgnoreCase(contentType)) return XHTML;
			if(startsWithIgnoreCase(contentType, CONTENT_TYPE_HTML + ';')) return HTML;
			if(startsWithIgnoreCase(contentType, CONTENT_TYPE_XHTML + ';')) return XHTML;
			throw new IllegalArgumentException("Unexpected value for contentType: " + contentType);
		}

		/**
		 * Gets the serialization represented of the given response.
		 *
		 * @see  #get(javax.servlet.ServletResponse)
		 */
		public static Serialization get(ServletResponse response) throws IllegalArgumentException {
			return get(response.getContentType());
		}

		/**
		 * Context init parameter that may be used to configure the use of XHTML within an application.
		 * Must be one of "html", "xhtml", or "auto" (the default).
		 */
		public static final String SELECT_INIT_PARAM = Serialization.class.getName() + ".select";

		/**
		 * Determine if the content may be served as <code>application/xhtml+xml</code> by the
		 * rules defined in <a href="http://www.w3.org/TR/xhtml-media-types/">http://www.w3.org/TR/xhtml-media-types/</a>
		 * Default to <code>application/xhtml+xml</code> as discussed at
		 * <a href="https://web.archive.org/web/20080913043830/http://www.smackthemouse.com/xhtmlxml">http://www.smackthemouse.com/xhtmlxml</a>
		 */
		public static Serialization select(ServletContext servletContext, HttpServletRequest request) {
			String initParam = servletContext.getInitParameter(SELECT_INIT_PARAM);
			if(initParam != null) {
				initParam = initParam.trim();
				if(!initParam.isEmpty()) {
					if("html".equalsIgnoreCase(initParam)) {
						return HTML;
					} else if("xhtml".equalsIgnoreCase(initParam)) {
						return XHTML;
					} else if(!"auto".equalsIgnoreCase(initParam)) {
						throw new IllegalArgumentException("Unexpected value for " + SELECT_INIT_PARAM + ": Must be one of \"html\", \"xhtml\", or \"auto\": " + initParam);
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
			boolean hasAcceptHtmlHtml = false;
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
									else if(acceptType.equalsIgnoreCase(CONTENT_TYPE_HTML)) hasAcceptHtmlHtml = true;
									else throw new AssertionError("Unexpected value for acceptType: " + acceptType);
								}
							}
						}
					}
				}
			}
			// If the Accept header explicitly contains application/xhtml+xml  (with either no "q" parameter or a positive "q" value) deliver the document using that media type.
			if(hasAcceptApplicationXhtmlXml) return XHTML;
			// If the Accept header explicitly contains text/html  (with either no "q" parameter or a positive "q" value) deliver the document using that media type.
			if(hasAcceptHtmlHtml) return HTML;
			// If the accept header contains "*/*" (a convention some user agents use to indicate that they will accept anything), deliver the document using text/html.
			if(hasAcceptStarStar) return HTML;
			// If has no accept headers
			if(!hasAcceptHeader) return XHTML;
			// This choice is not clear from either of the cited documents.  If there is an accept line,
			// and it doesn't have */* or application/xhtml+xml or text/html, we'll serve as text/html
			// since it is a fairly broken client anyway and would be even less likely to know xhtml.
			return HTML;
		}
	}

	public enum DocType {
		// See http://www.ibm.com/developerworks/library/x-think45/
		html5 {
			@Override
			public String getDocTypeLine(Serialization serialization) {
				return "<!DOCTYPE html>\n";
			}
		},
		strict {
			@Override
			public String getDocTypeLine(Serialization serialization) {
				switch(serialization) {
					case HTML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01//EN\" \"http://www.w3.org/TR/html4/strict.dtd\">\n";
					case XHTML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Strict//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-strict.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
		},
		transitional {
			@Override
			public String getDocTypeLine(Serialization serialization) {
				switch(serialization) {
					case HTML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Transitional//EN\" \"http://www.w3.org/TR/html4/loose.dtd\">\n";
					case XHTML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Transitional//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
		},
		frameset {
			@Override
			public String getDocTypeLine(Serialization serialization) {
				switch(serialization) {
					case HTML:
						return "<!DOCTYPE HTML PUBLIC \"-//W3C//DTD HTML 4.01 Frameset//EN\" \"http://www.w3.org/TR/html4/frameset.dtd\">\n";
					case XHTML:
						return "<!DOCTYPE html PUBLIC \"-//W3C//DTD XHTML 1.0 Frameset//EN\" \"http://www.w3.org/TR/xhtml1/DTD/xhtml1-frameset.dtd\">\n";
					default:
						throw new AssertionError();
				}
			}
		},
		none {
			@Override
			public void appendXmlDeclarationLine(Serialization serialization, String documentEncoding, Appendable out) {
				// Write nothing
			}
			@Override
			public String getDocTypeLine(Serialization serialization) {
				return "";
			}
		};

		private static boolean isUTF8(String documentEncoding) {
			return
				StandardCharsets.UTF_8.name().equalsIgnoreCase(documentEncoding)
				|| Charset.forName(documentEncoding) == StandardCharsets.UTF_8;
		}

		public void appendXmlDeclarationLine(Serialization serialization, String documentEncoding, Appendable out) throws IOException {
			if(serialization == Serialization.XHTML && !isUTF8(documentEncoding)) {
				out.append("<?xml version=\"1.0\" encoding=\"");
				encodeTextInXhtmlAttribute(documentEncoding, out);
				out.append("\"?>\n");
			}
		}

		/**
		 * See <a href="https://www.w3schools.com/tags/tag_doctype.asp">HTML doctype declaration</a>.
		 */
		public abstract String getDocTypeLine(Serialization serialization);
	}
}
