/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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

import org.springframework.util.Assert;

/**
 * @author Duncan McIntyre
 * @author Gary Russell
 * @since 1.4.1
 *
 */
public class RFC5424SyslogParser {

	public static final char NILVALUE = '-';

	public static final char SPACE = ' ';

	public final boolean retainOriginal;


	/**
	 * Construct a default parser; do not retain the origina unless there is
	 * an error.
	 */
	public RFC5424SyslogParser() {
		this(false);
	}

	/**
	 * @param retainOriginal when true, include the original content intact in the map.
	 */
	public RFC5424SyslogParser(boolean retainOriginal) {
		this.retainOriginal = retainOriginal;
	}

	public Map<String, ?> parse(String line, int octetCount, boolean shortRead) {

		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Reader r = new Reader(line);

		try {
			if (shortRead) {
				int n = line.length() - 1;
				while (n >=0 && line.charAt(n) == 0x00) {
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
			if(r.is(SPACE)) {
				r.getc();
				message = r.rest();
			}
			else {
				message = "";
			}

			int severity = pri & 0x7;
			int facility = pri >> 3;
			map.put(LogField.FACILITY.label(), facility);

			map.put(LogField.SEVERITY.label(), severity);
			map.put(LogField.SEVERITY_TEXT.label(), Severity.parseInt(severity).label());

			if (timestamp != null) {
				map.put(LogField.TIMESTAMP.label(), timestamp);
			}

			if (host != null) {
				map.put(LogField.HOST.label(), host);
			}
			if (app != null) {
				map.put(LogField.APP_NAME.label(), app);
			}
			if (procId != null) {
				map.put(LogField.PROCID.label(), procId);
			}
			if (msgId != null) {
				map.put(LogField.MSGID.label(), msgId);
			}
			map.put(LogField.VERSION.label(), version);

			if (structuredData != null) {
				map.put(LogField.STRUCTURED_DATA.label(), structuredData);
			}

			map.put(LogField.MESSAGE.label(), message);
			map.put(LogField.DECODE_ERRORS.label(), "false");

			if (this.retainOriginal) {
				map.put(LogField.UNDECODED.label(), line);
			}
		}
		catch(IllegalStateException e) {
			map.put(LogField.DECODE_ERRORS.label(), "true");
			map.put(LogField.ERRORS.label(), e.getMessage());
			map.put(LogField.UNDECODED.label(), line);
		}
		catch(StringIndexOutOfBoundsException sob) {
			map.put(LogField.DECODE_ERRORS.label(), "true");
			map.put(LogField.ERRORS.label(), "Unexpected end of message: " + sob.getMessage());
			map.put(LogField.UNDECODED.label(), line);
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

		if(c == NILVALUE) {
			return null;
		}

		if(!Character.isDigit(c)) {
			throw new IllegalStateException("Year expected @" + r.idx);
		}

		StringBuilder dateBuilder = new StringBuilder();
		dateBuilder.append((char) c);
		while ((c = r.getc()) != SPACE) {
			dateBuilder.append((char) c);
		}

		return dateBuilder.toString();
	}

	private Object getStructuredData(Reader r) {
		if(r.is(NILVALUE)) {
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
		List<String> fragments = new ArrayList<String>();
		while (r.is('[')) {
			r.mark();
			r.skipTo(']');
			fragments.add(r.getMarkedSegment());
		}
		return fragments;
	}

	public static class Reader {

		private final String line;

		public int idx;

		private int mark;

		public Reader(String l) {
			line = l;
		}

		public void mark() {
			this.mark = this.idx;
		}

		public String getMarkedSegment() {
			Assert.state(this.mark <= this.idx, "mark is greater than this.idx");
			return this.line.substring(mark, this.idx);
		}

		public int current() {
			return line.charAt(this.idx);
		}

		public int prev() {
			return line.charAt(this.idx - 1);
		}

		public int getc() {
			return line.charAt(this.idx++);
		}

		public int peek() {
			return line.charAt(this.idx + 1);
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
			return line.charAt(this.idx) == c;
		}

		public boolean was(char c) {
			return line.charAt(this.idx - 1) == c;
		}

		public boolean isDigit() {
			return Character.isDigit(line.charAt(this.idx));
		}

		public void expect(char c) {
			if (line.charAt(this.idx++) != c) {
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
			return line.substring(this.idx);
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

	/**
	 * An enumeration of all the fields produced by the system
	 *
	 */
	public enum LogField {
		// These correspond to generic syslog elements
		HOST("syslog_HOST"),
		FACILITY("syslog_FACILITY"),
		SEVERITY("syslog_SEVERITY"),
		TIMESTAMP("syslog_TIMESTAMP"),
		MESSAGE("syslog_MESSAGE"),

		// RFC5424
		APP_NAME("syslog_APP_NAME"),
		PROCID("syslog_PROCID"),
		MSGID("syslog_MSGID"),
		VERSION("syslog_VERSION"),
		STRUCTURED_DATA("syslog_STRUCTURED_DATA"),

		// Text versions of syslog numeric values
		SEVERITY_TEXT("syslog_SEVERITY_TEXT"),

		// Additional fields
		SOURCE_TYPE("syslog_SOURCE_TYPE"),
		SOURCE("syslog_SOURCE"),

		// full line when parse errors
		UNDECODED("syslog_UNDECODED"),

		DECODE_ERRORS("syslog_DECODE_ERRORS"),
		ERRORS("syslog_ERRORS")
		;

		private final String label;

		private LogField(String label) {
			this.label = label;
		}

		public String label() {
			return label;
		}

	}

	public enum Severity {

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

		private Severity(int level, String label) {
			this.level = level;
			this.label = label;
		}

		public int level() {
			return level;
		}

		public String label() {
			return label;
		}

		public static Severity parseInt(int syslogSeverity) {
			if(syslogSeverity == 7) {
				return DEBUG;
			}
			if(syslogSeverity == 6) {
				return INFO;
			}
			if(syslogSeverity == 5) {
				return NOTICE;
			}
			if(syslogSeverity == 4) {
				return WARN;
			}
			if(syslogSeverity == 3) {
				return ERROR;
			}
			if(syslogSeverity == 2) {
				return CRITICAL;
			}
			if(syslogSeverity == 1) {
				return ALERT;
			}
			if(syslogSeverity == 0) {
				return EMERGENCY;
			}
			return UNDEFINED;
		}
	}


}
