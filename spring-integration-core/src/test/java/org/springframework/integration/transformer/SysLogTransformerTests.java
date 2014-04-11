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

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class SysLogTransformerTests {

	@Test
	public void testMap() throws Exception {
		SyslogToMapTransformer t = new SyslogToMapTransformer();
		Map<String, ?> transformed = t.transformPayload(
				"<158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes());
		assertEquals(6, transformed.size());
		assertEquals(19, transformed.get(SyslogToMapTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogToMapTransformer.SEVERITY));
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", transformed.get(SyslogToMapTransformer.HOST));
		assertEquals("TESTING[70729]", transformed.get(SyslogToMapTransformer.TAG));
		assertEquals("TEST SYSLOG MESSAGE", transformed.get(SyslogToMapTransformer.MESSAGE));

		String[] fields = new String[] {SyslogToMapTransformer.FACILITY,
				SyslogToMapTransformer.SEVERITY, SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.TAG, SyslogToMapTransformer.MESSAGE};
		Object[] values = new Object[] {19, 6, date, "WEBERN", "TESTING[70729]", "TEST SYSLOG MESSAGE"};
		// check iteration order
		int n = 0;
		for (Entry<String, ?> entry : transformed.entrySet()) {
			assertEquals(fields[n++], entry.getKey());
		}
		n = 0;
		for (String key : transformed.keySet()) {
			assertEquals(fields[n++], key);
		}
		n = 0;
		for (Object value : transformed.values()) {
			assertEquals(values[n++], value);
		}
	}

	@Test
	public void testBadPattern() throws Exception {
		SyslogToMapTransformer t = new SyslogToMapTransformer();
		String syslog = "&158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = t.transformPayload(
				syslog.getBytes());
		assertEquals(1, transformed.size());
		assertEquals(syslog, transformed.get(SyslogToMapTransformer.UNDECODED));
	}

	@Test
	public void testBadFacilitySeverity() throws Exception {
		SyslogToMapTransformer t = new SyslogToMapTransformer();
		String syslog = "<X58>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = t.transformPayload(
				syslog.getBytes());
		assertEquals(1, transformed.size());
		assertEquals(syslog, transformed.get(SyslogToMapTransformer.UNDECODED));
	}

	@Test
	public void testWithoutTag() throws Exception {
		SyslogToMapTransformer t = new SyslogToMapTransformer();
		Map<String, ?> transformed = t.transformPayload(
				"<158>JUL 26 22:08:35 WEBERN TEST SYSLOG MESSAGE".getBytes());
		assertEquals(5, transformed.size());
		assertEquals(19, transformed.get(SyslogToMapTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogToMapTransformer.SEVERITY));
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", transformed.get(SyslogToMapTransformer.HOST));
		assertFalse(transformed.containsKey(SyslogToMapTransformer.TAG));
		assertEquals("TEST SYSLOG MESSAGE", transformed.get(SyslogToMapTransformer.MESSAGE));

		String[] fields = new String[] {SyslogToMapTransformer.FACILITY,
				SyslogToMapTransformer.SEVERITY, SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.MESSAGE};

		Object[] values = new Object[] {19, 6, date, "WEBERN", "TEST SYSLOG MESSAGE"};
		// check iteration order
		int n = 0;
		for (Entry<String, ?> entry : transformed.entrySet()) {
			assertEquals(fields[n++], entry.getKey());
		}
		n = 0;
		for (String key : transformed.keySet()) {
			assertEquals(fields[n++], key);
		}
		n = 0;
		for (Object value : transformed.values()) {
			assertEquals(values[n++], value);
		}
	}

}
