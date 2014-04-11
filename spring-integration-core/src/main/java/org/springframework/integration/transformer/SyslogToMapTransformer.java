/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.transformer;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Transforms a packet in Syslog (RFC3164) format to a Map.
 * If the packet cannot be decoded, the entire packet
 * is returned as a String under the key {@code UNDECODED}. If the date field can be
 * parsed, it will be returned as a {@link Date} object; otherwise it is returned as a String.
 *
 * @author Gary Russell
 * @author Artem Bilan
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

	private final Pattern pattern = Pattern.compile("<([^>]+)>(.{15}) ([^ ]+) (?:([^:]+): )?(.*)", Pattern.DOTALL);

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

	@Override
	protected Map<String, ?> transformPayload(Object payload) throws Exception {
		boolean isByteArray = payload instanceof byte[];
		boolean isString = payload instanceof String;
		Assert.isTrue(isByteArray || isString, "payload must be String or byte[]");
		if (isByteArray) {
			return this.transform((byte[]) payload);
		}
		else if (isString) {
			return this.transform((String) payload);
		}
		return null;
	}

	private Map<String, ?> transform(byte[] payloadBytes) {
		String payload;
		try {
			payload = new String(payloadBytes, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			payload = new String(payloadBytes);
		}
		return transform(payload);
	}

	private Map<String, ?> transform(String payload) {
		Map<String, Object> map = new LinkedHashMap<String, Object>();
		Matcher matcher = pattern.matcher(payload);
		if (matcher.matches()) {
			try {
				String facilityString = matcher.group(1);
				int facility = Integer.parseInt(facilityString);
				int severity = facility & 0x7;
				facility = facility >> 3;
				map.put(FACILITY, facility);
				map.put(SEVERITY, severity);
				String timestamp = matcher.group(2);
				Date date;
				try {
					date = this.dateFormat.parse(timestamp);
					Calendar calendar = Calendar.getInstance();
					int year = calendar.get(Calendar.YEAR);
					int month = calendar.get(Calendar.MONTH);
					calendar.setTime(date);
					/*
					 * syslog date doesn't include a year so we
					 * need to insert the current year - adjusted
					 * if necessary if close to midnight on Dec 31.
					 */
					if (month == 11 && calendar.get(Calendar.MONTH) == 0) {
						calendar.set(Calendar.YEAR, year + 1);
					}
					else if (month == 0 && calendar.get(Calendar.MONTH) == 1) {
						calendar.set(Calendar.YEAR, year - 1);
					}
					else {
						calendar.set(Calendar.YEAR, year);
					}
					map.put(TIMESTAMP, calendar.getTime());
				}
				catch (Exception e) {
					/*
					 * If we can't parse the timestamp, return it as an
					 * unmodified String. (Postel's law).
					 */
					map.put(TIMESTAMP, timestamp);
				}
				map.put(HOST, matcher.group(3));
				if (matcher.group(4) != null) {
					map.put(TAG, matcher.group(4));
				}
				map.put(MESSAGE, matcher.group(5));
			}
			catch (Exception e) {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not decode:" + payload, e);
				}
				map.clear();
				map.put(UNDECODED, payload);
			}
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("Could not decode:" + payload);
			}
			map.put(UNDECODED, payload);
		}
		return map;
	}

}
