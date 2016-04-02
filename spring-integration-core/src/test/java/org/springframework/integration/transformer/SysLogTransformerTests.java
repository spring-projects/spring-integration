/*
 * Copyright 2002-2015 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Karol Dowbecki
 * @since 2.2
 */
public class SysLogTransformerTests {

	private final SyslogToMapTransformer sut = new SyslogToMapTransformer();

	@Test
	public void testMap() throws Exception {
		String syslog = "<158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertEquals(6, transformed.size());
		assertEquals(19, transformed.get(SyslogToMapTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogToMapTransformer.SEVERITY));
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", transformed.get(SyslogToMapTransformer.HOST));
		assertEquals("TESTING", transformed.get(SyslogToMapTransformer.TAG));
		assertEquals("[70729]: TEST SYSLOG MESSAGE", transformed.get(SyslogToMapTransformer.MESSAGE));

		String[] fields = {SyslogToMapTransformer.FACILITY, SyslogToMapTransformer.SEVERITY,
				SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.TAG, SyslogToMapTransformer.MESSAGE};
		Object[] values = {19, 6, date, "WEBERN", "TESTING", "[70729]: TEST SYSLOG MESSAGE"};
		assertIterationOrder(fields, values, transformed);
	}

	@Test
	public void testBadPattern() throws Exception {
		String syslog = "&158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertEquals(1, transformed.size());
		assertEquals(syslog, transformed.get(SyslogToMapTransformer.UNDECODED));
	}

	@Test
	public void testBadFacilitySeverity() throws Exception {
		String syslog = "<X58>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertEquals(1, transformed.size());
		assertEquals(syslog, transformed.get(SyslogToMapTransformer.UNDECODED));
	}

	@Test
	public void testWithoutTag() throws Exception {
		String syslog = "<158>JUL 26 22:08:35 WEBERN [70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertEquals(5, transformed.size());
		assertEquals(19, transformed.get(SyslogToMapTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogToMapTransformer.SEVERITY));
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", transformed.get(SyslogToMapTransformer.HOST));
		assertFalse(transformed.containsKey(SyslogToMapTransformer.TAG));
		assertEquals("[70729]: TEST SYSLOG MESSAGE", transformed.get(SyslogToMapTransformer.MESSAGE));

		String[] fields = {SyslogToMapTransformer.FACILITY,	SyslogToMapTransformer.SEVERITY,
				SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.MESSAGE};
		Object[] values = {19, 6, date, "WEBERN", "[70729]: TEST SYSLOG MESSAGE"};
		assertIterationOrder(fields, values, transformed);
	}

	@Test
	public void testTagMaxLength() throws Exception {
		String syslog = "<158>JUL 26 22:08:35 WEBERN ABCDE1234567890ABCDE1234567890UVXYZ TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertEquals(6, transformed.size());
		assertEquals(19, transformed.get(SyslogToMapTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogToMapTransformer.SEVERITY));
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertTrue(date instanceof Date || date instanceof String);
		assertEquals("WEBERN", transformed.get(SyslogToMapTransformer.HOST));
		assertEquals("ABCDE1234567890ABCDE1234567890UV", transformed.get(SyslogToMapTransformer.TAG));
		assertEquals("XYZ TEST SYSLOG MESSAGE", transformed.get(SyslogToMapTransformer.MESSAGE));

		String[] fields = {SyslogToMapTransformer.FACILITY, SyslogToMapTransformer.SEVERITY,
				SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.TAG, SyslogToMapTransformer.MESSAGE};
		Object[] values = {19, 6, date, "WEBERN", "ABCDE1234567890ABCDE1234567890UV", "XYZ TEST SYSLOG MESSAGE"};
		assertIterationOrder(fields, values, transformed);
	}

	private static void assertIterationOrder(String[] expectedFields, Object[] expectedValues,
											 Map<String, ?> actualTransformed) {
		int n = 0;
		for (Entry<String, ?> entry : actualTransformed.entrySet()) {
			assertEquals(expectedFields[n++], entry.getKey());
		}
		n = 0;
		for (String key : actualTransformed.keySet()) {
			assertEquals(expectedFields[n++], key);
		}
		n = 0;
		for (Object value : actualTransformed.values()) {
			assertEquals(expectedValues[n++], value);
		}
	}

}
