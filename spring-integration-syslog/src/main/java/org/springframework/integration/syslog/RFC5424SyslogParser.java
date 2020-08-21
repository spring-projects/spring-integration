/*
 * Copyright 2014-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.syslog;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.integration.util.JavaUtils;
import org.springframework.util.Assert;

/**
 * Parse for RFC 5424 syslog messages; when used with TCP, requires the use
 * of a {@code RFC6587SyslogDeserializer} which decodes the framing.
 *
 * @author Duncan McIntyre
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1.1
 *
 */
public class RFC5424SyslogParser {

	protected static final char NILVALUE = '-';

	protected static final char SPACE = ' ';

	protected final boolean retainOriginal; // NOSONAR final


	/**
	 * Construct a default parser; do not retain the original message content unless there
	 * is an error.
	 */
	public RFC5424SyslogParser() {
		this(false);
	}

	/**
	 * @param retainOriginal when true, include the original message content intact in the
	 * map.
	 */
	public RFC5424SyslogParser(boolean retainOriginal) {
		this.retainOriginal = retainOriginal;
	}

	public Map<String, ?> parse(String lineArg, int octetCount, boolean shortRead) {
		Map<String, Object> map = new LinkedHashMap<>();
		String line = lineArg;
		Reader r = new Reader(line);

		try {
			if (shortRead) {
				int n = line.length() - 1;
				while (n >= 0 && line.charAt(n) == 0x00) {
					n--;
				}
				line = line.substring(0, n);
				throw new IllegalStateException("Insufficient data; expected " + octetCount + " got " + (n + 1));
			}
			r.expect('<');
			int pri = r.readInt();
			r.expect('>');

			int version = r.readInt();
			r.expect(SPACE);

			Object timestamp = getTimestamp(r);

			String host = r.getIdentifier();
			String app = r.getIdentifier();
			String procId = r.getIdentifier();
			String msgId = r.getIdentifier();

			Object structuredData = getStructuredData(r);

			String message;
			if (r.is(SPACE)) {
				r.getc();
				message = r.rest();
			}
			else {
				message = "";
			}

			int severity = pri & 0x7;
			int facility = pri >> 3;
			map.put(SyslogHeaders.FACILITY, facility);
			map.put(SyslogHeaders.SEVERITY, severity);
			map.put(SyslogHeaders.SEVERITY_TEXT, Severity.parseInt(severity).label());
			map.put(SyslogHeaders.VERSION, version);
			map.put(SyslogHeaders.MESSAGE, message);
			map.put(SyslogHeaders.DECODE_ERRORS, "false");

			JavaUtils.INSTANCE
					.acceptIfNotNull(timestamp, (value) -> map.put(SyslogHeaders.TIMESTAMP, value))
					.acceptIfNotNull(host, (value) -> map.put(SyslogHeaders.HOST, value))
					.acceptIfNotNull(app, (value) -> map.put(SyslogHeaders.APP_NAME, value))
					.acceptIfNotNull(procId, (value) -> map.put(SyslogHeaders.PROCID, value))
					.acceptIfNotNull(msgId, (value) -> map.put(SyslogHeaders.MSGID, value))
					.acceptIfNotNull(structuredData, (value) -> map.put(SyslogHeaders.STRUCTURED_DATA, value))
					.acceptIfCondition(this.retainOriginal, line, (value) -> map.put(SyslogHeaders.UNDECODED, value));
		}
		catch (IllegalStateException | StringIndexOutOfBoundsException ex) {
			map.put(SyslogHeaders.DECODE_ERRORS, "true");
			map.put(SyslogHeaders.ERRORS,
					(ex instanceof StringIndexOutOfBoundsException ? "Unexpected end of message: " : "") // NOSONAR
							+ ex.getMessage());
			map.put(SyslogHeaders.UNDECODED, line);
		}
		return map;
	}

	/**
	 * Default implementation returns the date as a String (if present).
	 * @param r the reader.
	 * @return the timestamp.
	 */
	protected Object getTimestamp(Reader r) {

		int c = r.getc();

		if (c == NILVALUE) {
			return null;
		}

		if (!Character.isDigit(c)) {
			throw new IllegalStateException("Year expected @" + r.getIndex());
		}

		StringBuilder dateBuilder = new StringBuilder();
		dateBuilder.append((char) c);
		while ((c = r.getc()) != SPACE) {
			dateBuilder.append((char) c);
		}

		return dateBuilder.toString();
	}

	private Object getStructuredData(Reader r) {
		if (r.is(NILVALUE)) {
			r.getc();
			return null;
		}
		return parseStructuredDataElements(r);
	}

	/**
	 * Default implementation returns a list of structured data elements with
	 * no internal parsing.
	 * @param r the reader.
	 * @return the structured data.
	 */
	protected Object parseStructuredDataElements(Reader r) {
		List<String> fragments = new ArrayList<>();
		while (r.is('[')) {
			r.mark();
			r.skipTo(']');
			fragments.add(r.getMarkedSegment());
		}
		return fragments;
	}

	protected static class Reader {

		private final String line;

		private int idx;

		private int mark;

		public Reader(String l) {
			this.line = l;
		}

		public int getIndex() {
			return this.idx;
		}

		public void mark() {
			this.mark = this.idx;
		}

		public String getMarkedSegment() {
			Assert.state(this.mark <= this.idx, "mark is greater than this.idx");
			return this.line.substring(this.mark, this.idx);
		}

		public int current() {
			return this.line.charAt(this.idx);
		}

		public int prev() {
			return this.line.charAt(this.idx - 1);
		}

		public int getc() {
			return this.line.charAt(this.idx++);
		}

		public int peek() {
			return this.line.charAt(this.idx + 1);
		}

		public void ungetc() {
			this.idx--;
		}

		public int getInt() {
			int c = getc();
			if (!Character.isDigit(c)) {
				ungetc();
				return -1;
			}

			return c - '0';
		}

		/**
		 * Read characters building an int until a non-digit is found
		 * @return int
		 */
		public int readInt() {
			int val = 0;
			while (isDigit()) {
				val = (val * 10) + getInt();
			}
			return val;
		}

		public double readFraction() {
			int val = 0;
			int order = 0;
			while (isDigit()) {
				val = (val * 10) + getInt();
				order *= 10;
			}
			return (double) val / order;

		}

		public boolean is(char c) {
			return this.line.charAt(this.idx) == c;
		}

		public boolean was(char c) {
			return this.line.charAt(this.idx - 1) == c;
		}

		public boolean isDigit() {
			return Character.isDigit(this.line.charAt(this.idx));
		}

		public void expect(char c) {
			if (this.line.charAt(this.idx++) != c) {
				throw new IllegalStateException("Expected '" + c + "' @" + this.idx);
			}
		}

		public void skipTo(char searchChar) {
			while (!is(searchChar) || was('\\')) {
				getc();
			}
			getc();
		}

		public String rest() {
			return this.line.substring(this.idx);
		}

		public String getIdentifier() {
			StringBuilder sb = new StringBuilder();
			int c;
			while (true) {
				c = getc();
				if (c >= 33 && c <= 127) {
					sb.append((char) c);
				}
				else {
					break;
				}
			}
			return sb.toString();
		}

	}

	protected enum Severity {

		DEBUG(7, "DEBUG"),

		INFO(6, "INFO"),

		NOTICE(5, "NOTICE"),

		WARN(4, "WARN"),

		ERROR(3, "ERRORS"),

		CRITICAL(2, "CRITICAL"),

		ALERT(1, "ALERT"),

		EMERGENCY(0, "EMERGENCY"),

		UNDEFINED(-1, "UNDEFINED");

		private final int level;

		private final String label;

		Severity(int level, String label) {
			this.level = level;
			this.label = label;
		}

		public int level() {
			return this.level;
		}

		public String label() {
			return this.label;
		}

		public static Severity parseInt(int syslogSeverity) {
			if (syslogSeverity == 7) {
				return DEBUG;
			}
			if (syslogSeverity == 6) {
				return INFO;
			}
			if (syslogSeverity == 5) {
				return NOTICE;
			}
			if (syslogSeverity == 4) {
				return WARN;
			}
			if (syslogSeverity == 3) {
				return ERROR;
			}
			if (syslogSeverity == 2) {
				return CRITICAL;
			}
			if (syslogSeverity == 1) {
				return ALERT;
			}
			if (syslogSeverity == 0) {
				return EMERGENCY;
			}
			return UNDEFINED;
		}

	}


}
