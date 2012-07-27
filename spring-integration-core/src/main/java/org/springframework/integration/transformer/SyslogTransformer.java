/*
 * Copyright 2002-2012 the original author or authors.
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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Transforms a packet in Syslog (RFC5424) format to either a Map (default)
 * or List of fields in the order received. If the packet cannot be decoded, the entire packet
 * is returned as a String (under the key UNDECODED if a map). If the date field can be
 * parsed, it will be returned as a {@link Date} object; otherwise it is returned as a String.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SyslogTransformer extends AbstractPayloadTransformer<Object, Object> {

	public static final String FACILITY = "FACILITY";

	public static final String SEVERITY = "SEVERITY";

	public static final String TIMESAMP = "TIMESTAMP";

	public static final String HOST = "HOST";

	public static final String TAG = "TAG";

	public static final String MESSAGE = "MESSAGE";

	public static final String UNDECODED = "UNDECODED";

	private final Pattern pattern = Pattern.compile("<([^>]+)>(.{15}) ([^ ]+) ([^:]+): (.*)", Pattern.DOTALL);

	private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd HH:mm:ss");

	private volatile boolean asMap = true;

	/**
	 * Transform to a Map or a List (default Map).
	 * @param asMap If true, transform to a Map.
	 */
	public void setAsMap(boolean asMap) {
		this.asMap = asMap;
	}

	private Object transform(byte[] payloadBytes) {
		String payload;
		try {
			payload = new String(payloadBytes, "UTF-8");
		}
		catch (UnsupportedEncodingException e) {
			payload = new String(payloadBytes);
		}
		return transform(payload);
	}

	private Object transform(String payload) {
		Map<String, Object> map;
		List<?> tuple = this.payloadToList(payload);
		if (this.asMap) {
			map = new HashMap<String, Object>();
			if(tuple.size() == 6) {
				map.put(FACILITY, tuple.get(0));
				map.put(SEVERITY, tuple.get(1));
				map.put(TIMESAMP, tuple.get(2));
				map.put(HOST, tuple.get(3));
				map.put(TAG, tuple.get(4));
				map.put(MESSAGE, tuple.get(5));
			}
			else if(tuple.size() == 1) {
				map.put(UNDECODED, tuple.get(0));
			}
			else {
				throw new MessageTransformationException("Unexpected tuple size " + tuple.size());
			}
			return map;
		}
		else {
			return tuple;
		}
	}

	protected final List<?> payloadToList(String payload) {
		Matcher matcher = this.pattern.matcher(payload);
		List<Object> tuple;
		try {
			if (matcher.matches()) {
				tuple = new ArrayList<Object>(matcher.groupCount() + 1);
				for (int i = 1; i <= matcher.groupCount(); i++) {
					if (i == 1) {
						String facilityString = matcher.group(1);
						int facility = Integer.parseInt(facilityString);
						int severity = facility & 0x7;
						facility = facility >> 3;
						tuple.add(facility);
						tuple.add(severity);
					}
					else if (i == 2) {
						String timestamp = matcher.group(i);
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
							tuple.add(calendar.getTime());
						}
						catch (Exception e) {
							tuple.add(timestamp);
						}
					}
					else {
						tuple.add(matcher.group(i));
					}
				}
			}
			else {
				if (logger.isDebugEnabled()) {
					logger.debug("Could not decode:" + payload);
				}
				tuple = new ArrayList<Object>(1);
				tuple.add(payload);
			}
		}
		catch (Exception e) {
			tuple = new ArrayList<Object>(1);
			tuple.add(payload);
		}
		return tuple;
	}

	@Override
	protected Object transformPayload(Object payload) throws Exception {
		if (payload instanceof byte[]) {
			return this.transform((byte[]) payload);
		}
		else if (payload instanceof String) {
			return this.transform((String) payload);
		}
		else {
			Assert.isTrue(false, "payload must be String or byte[]");
			return null;
		}
	}
}
