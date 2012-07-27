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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.junit.Test;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SysLogTransformerTests {

	@Test
	public void testMap() throws Exception {
		SyslogTransformer t = new SyslogTransformer();
		@SuppressWarnings("unchecked")
		Map<String, ?> transformed = (Map<String, String>) t.transformPayload(
				"<158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes());
		assertEquals(6, transformed.size());
//		System.out.println(transformed);
		assertEquals(19, transformed.get(SyslogTransformer.FACILITY));
		assertEquals(6, transformed.get(SyslogTransformer.SEVERITY));
		assertTrue(transformed.get(SyslogTransformer.TIMESAMP) instanceof Date);
		assertEquals("WEBERN", transformed.get(SyslogTransformer.HOST));
		assertEquals("TESTING[70729]", transformed.get(SyslogTransformer.TAG));
		assertEquals("TEST SYSLOG MESSAGE", transformed.get(SyslogTransformer.MESSAGE));
	}

	@Test
	public void testList() throws Exception {
		SyslogTransformer t = new SyslogTransformer();
		t.setAsMap(false);
		@SuppressWarnings("unchecked")
		List<?> transformed = (List<String>) t.transformPayload(
				"<158>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE".getBytes());
		assertEquals(6, transformed.size());
//		System.out.println(transformed);
		assertEquals(19, transformed.get(0));
		assertEquals(6, transformed.get(1));
		assertTrue(transformed.get(2) instanceof Date);
		assertEquals("WEBERN", transformed.get(3));
		assertEquals("TESTING[70729]", transformed.get(4));
		assertEquals("TEST SYSLOG MESSAGE", transformed.get(5));
	}

}
