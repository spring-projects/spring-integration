/*
 * Copyright 2002-2022 the original author or authors.
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

import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
		assertThat(transformed.size()).isEqualTo(6);
		assertThat(transformed.get(SyslogToMapTransformer.FACILITY)).isEqualTo(19);
		assertThat(transformed.get(SyslogToMapTransformer.SEVERITY)).isEqualTo(6);
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertThat(date instanceof Date || date instanceof String).isTrue();
		assertThat(transformed.get(SyslogToMapTransformer.HOST)).isEqualTo("WEBERN");
		assertThat(transformed.get(SyslogToMapTransformer.TAG)).isEqualTo("TESTING");
		assertThat(transformed.get(SyslogToMapTransformer.MESSAGE)).isEqualTo("[70729]: TEST SYSLOG MESSAGE");

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
		assertThat(transformed.size()).isEqualTo(1);
		assertThat(transformed.get(SyslogToMapTransformer.UNDECODED)).isEqualTo(syslog);
	}

	@Test
	public void testBadFacilitySeverity() throws Exception {
		String syslog = "<X58>JUL 26 22:08:35 WEBERN TESTING[70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertThat(transformed.size()).isEqualTo(1);
		assertThat(transformed.get(SyslogToMapTransformer.UNDECODED)).isEqualTo(syslog);
	}

	@Test
	public void testWithoutTag() throws Exception {
		String syslog = "<158>JUL 26 22:08:35 WEBERN [70729]: TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertThat(transformed.size()).isEqualTo(5);
		assertThat(transformed.get(SyslogToMapTransformer.FACILITY)).isEqualTo(19);
		assertThat(transformed.get(SyslogToMapTransformer.SEVERITY)).isEqualTo(6);
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertThat(date instanceof Date || date instanceof String).isTrue();
		assertThat(transformed.get(SyslogToMapTransformer.HOST)).isEqualTo("WEBERN");
		assertThat(transformed.containsKey(SyslogToMapTransformer.TAG)).isFalse();
		assertThat(transformed.get(SyslogToMapTransformer.MESSAGE)).isEqualTo("[70729]: TEST SYSLOG MESSAGE");

		String[] fields = {SyslogToMapTransformer.FACILITY, SyslogToMapTransformer.SEVERITY,
				SyslogToMapTransformer.TIMESTAMP, SyslogToMapTransformer.HOST,
				SyslogToMapTransformer.MESSAGE};
		Object[] values = {19, 6, date, "WEBERN", "[70729]: TEST SYSLOG MESSAGE"};
		assertIterationOrder(fields, values, transformed);
	}

	@Test
	public void testTagMaxLength() throws Exception {
		String syslog = "<158>JUL 26 22:08:35 WEBERN ABCDE1234567890ABCDE1234567890UVXYZ TEST SYSLOG MESSAGE";
		Map<String, ?> transformed = sut.transformPayload(syslog.getBytes());
		assertThat(transformed.size()).isEqualTo(6);
		assertThat(transformed.get(SyslogToMapTransformer.FACILITY)).isEqualTo(19);
		assertThat(transformed.get(SyslogToMapTransformer.SEVERITY)).isEqualTo(6);
		Object date = transformed.get(SyslogToMapTransformer.TIMESTAMP);
		assertThat(date instanceof Date || date instanceof String).isTrue();
		assertThat(transformed.get(SyslogToMapTransformer.HOST)).isEqualTo("WEBERN");
		assertThat(transformed.get(SyslogToMapTransformer.TAG)).isEqualTo("ABCDE1234567890ABCDE1234567890UV");
		assertThat(transformed.get(SyslogToMapTransformer.MESSAGE)).isEqualTo("XYZ TEST SYSLOG MESSAGE");

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
			assertThat(entry.getKey()).isEqualTo(expectedFields[n++]);
		}
		n = 0;
		for (String key : actualTransformed.keySet()) {
			assertThat(key).isEqualTo(expectedFields[n++]);
		}
		n = 0;
		for (Object value : actualTransformed.values()) {
			assertThat(value).isEqualTo(expectedValues[n++]);
		}
	}

}
