/*
 * ao-servlet-util - Miscellaneous Servlet and JSP utilities.
 * Copyright (C) 2013, 2014, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.lang.io.NoClose;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;

/**
 * Discards all data.
 */
public final class NullServletOutputStream extends ServletOutputStream implements NoClose {

	private static final NullServletOutputStream instance = new NullServletOutputStream();

	public static NullServletOutputStream getInstance() {
		return instance;
	}

	private NullServletOutputStream() {
		// Do nothing
	}

	@Override
	public void close() {
		// Do nothing
	}

	@Override
	public void flush() {
		// Do nothing
	}

	@Override
	public void write(byte[] b) {
		// Discard all
	}

	@Override
	public void write(byte[] b, int off, int len) {
		// Discard all
	}

	@Override
	public void write(int b) {
		// Discard all
	}

	@Override
	public void print(String s) {
		// Discard all
	}

	@Override
	public void print(boolean b) {
		// Discard all
	}

	@Override
	public void print(char c) {
		// Discard all
	}

	@Override
	public void print(int i) {
		// Discard all
	}

	@Override
	public void print(long l) {
		// Discard all
	}

	@Override
	public void print(float f) {
		// Discard all
	}

	@Override
	public void print(double d) {
		// Discard all
	}

	@Override
	public void println() {
		// Discard all
	}

	@Override
	public void println(String s) {
		// Discard all
	}

	@Override
	public void println(boolean b) {
		// Discard all
	}

	@Override
	public void println(char c) {
		// Discard all
	}

	@Override
	public void println(int i) {
		// Discard all
	}

	@Override
	public void println(long l) {
		// Discard all
	}

	@Override
	public void println(float f) {
		// Discard all
	}

	@Override
	public void println(double d) {
		// Discard all
	}

	@Override
	public boolean isReady() {
		return true;
	}

	@Override
	public void setWriteListener(WriteListener wl) {
		throw new IllegalStateException("Implement when first required");
		/*
		try {
			wl.onWritePossible();
		} catch(IOException e) {
			wl.onError(e);
		}*/
	}
}
