/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.transformer;

import java.nio.charset.StandardCharsets;
import java.sql.Date;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Transforms a packet in Syslog (RFC3164) format to a Map.
 * If the packet cannot be decoded, the entire packet
 * is returned as a String under the key {@code UNDECODED}. If the date field can be
 * parsed, it will be returned as a {@link Date} object; otherwise it is returned as a String.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Karol Dowbecki
 *
 * @since 2.2
 *
 */
public class SyslogToMapTransformer extends AbstractPayloadTransformer<Object, Map<String, ?>> {

	public static final String FACILITY = "FACILITY";

	public static final String SEVERITY = "SEVERITY";

	public static final String TIMESTAMP = "TIMESTAMP";

	public static final String HOST = "HOST";

	public static final String TAG = "TAG";

	public static final String MESSAGE = "MESSAGE";

	public static final String UNDECODED = "UNDECODED";

	private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("MMM dd HH:mm:ss");

	private final Pattern pattern = Pattern.compile("<([^>]+)>(.{15}) ([^ ]+) ([a-zA-Z0-9]{0,32})(.*)", Pattern.DOTALL);

	@Override
	protected Map<String, ?> transformPayload(Object payload) {
		boolean isByteArray = payload instanceof byte[];
		boolean isString = payload instanceof String;
		Assert.isTrue(isByteArray || isString, "payload must be String or byte[]");
		if (isByteArray) {
			return this.transform((byte[]) payload);
		}
		else {
			return this.transform((String) payload);
		}
	}

	private Map<String, ?> transform(byte[] payloadBytes) {
		return transform(new String(payloadBytes, StandardCharsets.UTF_8));
	}

	private Map<String, ?> transform(String payload) {
		Map<String, Object> map = new LinkedHashMap<>();
		Matcher matcher = this.pattern.matcher(payload);
		if (matcher.matches()) {
			parseMatcherToMap(payload, matcher, map);
		}
		else {
			logger.debug(() -> "Could not decode: " + payload);
			map.put(UNDECODED, payload);
		}
		return map;
	}

	private void parseMatcherToMap(Object payload, Matcher matcher, Map<String, Object> map) {
		try {
			String facilityString = matcher.group(1); // NOSONAR
			int facility = Integer.parseInt(facilityString);
			int severity = facility & 0x7; // NOSONAR
			facility = facility >> 3; // NOSONAR
			map.put(FACILITY, facility);
			map.put(SEVERITY, severity);
			String timestamp = matcher.group(2); // NOSONAR
			parseTimestampToMap(timestamp, map);
			map.put(HOST, matcher.group(3)); // NOSONAR
			String tag = matcher.group(4); // NOSONAR
			if (StringUtils.hasLength(tag)) {
				map.put(TAG, tag);
			}
			map.put(MESSAGE, matcher.group(5)); // NOSONAR
		}
		catch (Exception ex) {
			logger.debug(ex, () -> "Could not decode: " + payload);
			map.clear();
			map.put(UNDECODED, payload);
		}
	}

	private void parseTimestampToMap(String timestamp, Map<String, Object> map) {
		try {
			LocalDate localDate = this.dateTimeFormatter.parse(timestamp, LocalDate::from);
			Calendar calendar = Calendar.getInstance();
			int year = calendar.get(Calendar.YEAR);
			int month = calendar.get(Calendar.MONTH);
			calendar.setTime(Date.valueOf(localDate));
			/*
			 * syslog date doesn't include a year so we
			 * need to insert the current year - adjusted
			 * if necessary if close to midnight on Dec 31.
			 */
			if (month == Calendar.DECEMBER && calendar.get(Calendar.MONTH) == Calendar.JANUARY) {
				calendar.set(Calendar.YEAR, year + 1);
			}
			else if (month == Calendar.JANUARY && calendar.get(Calendar.MONTH) == Calendar.FEBRUARY) {
				calendar.set(Calendar.YEAR, year - 1);
			}
			else {
				calendar.set(Calendar.YEAR, year);
			}
			map.put(TIMESTAMP, calendar.getTime());
		}
		catch (@SuppressWarnings("unused") Exception e) {
			/*
			 * If we can't parse the timestamp, return it as an
			 * unmodified String. (Postel's law).
			 */
			map.put(TIMESTAMP, timestamp);
		}
	}

}
