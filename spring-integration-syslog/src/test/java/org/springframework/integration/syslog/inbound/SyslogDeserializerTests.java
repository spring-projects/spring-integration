/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.syslog.inbound;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.syslog.SyslogHeaders;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Duncan McIntyre
 * @author Gary Russell
 * @since 4.1.1
 *
 */
public class SyslogDeserializerTests {

	static final String VALID_UNFRAMED_ENTRY =
			"<14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - - Removing instance\n";

	static final String VALID_FRAMED_ENTRY =
			"106 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - - Removing instance";

	static final String SHORT_FRAMED_ENTRY =
			"107 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - - Removing instance";

	static final String SD_ENTRY_1 =
			"179 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - " +
					"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"] Removing instance";

	static final String SD_ENTRY_2 =
			"253 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - " +
					"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"][exampleSDID@32473 " +
					"iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"] Removing instance";

	static final String SD_ENTRY_3 = "275 <14>1 2014-06-20T09:14:07+00:00 loggregator d0602076-b14a-4c55-852a-981e7afeed38 DEA - " +
			"[exampleSDID@32473 iut=\\\"3\\\" eventSource=\\\"Application\\\" eventID=\\\"1011\\\"][exampleSDID@32473 " +
			"iut=\\\"3\\\" eventSource=\\\"Application\\\" escapedBracket=\\\"\\]\\\" eventID=\\\"1011\\\"] Removing instance";

	@Test
	public void shouldParseAValidFramedEntry() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(VALID_FRAMED_ENTRY.getBytes()));

		assertThat(map.get(SyslogHeaders.FACILITY)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.SEVERITY)).isEqualTo(6);
		assertThat(map.get(SyslogHeaders.VERSION)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.TIMESTAMP)).isEqualTo("2014-06-20T09:14:07+00:00");
		assertThat(map.get(SyslogHeaders.HOST)).isEqualTo("loggregator");
		assertThat(map.get(SyslogHeaders.APP_NAME)).isEqualTo("d0602076-b14a-4c55-852a-981e7afeed38");
		assertThat(map.get(SyslogHeaders.PROCID)).isEqualTo("DEA");
		assertThat(map.get(SyslogHeaders.MSGID)).isEqualTo("-");
		assertThat(map.get(SyslogHeaders.MESSAGE)).isEqualTo("Removing instance");
	}

	@Test
	public void shouldParseAValidUnframedEntry() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(VALID_UNFRAMED_ENTRY.getBytes()));

		assertThat(map.get(SyslogHeaders.FACILITY)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.SEVERITY)).isEqualTo(6);
		assertThat(map.get(SyslogHeaders.VERSION)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.TIMESTAMP)).isEqualTo("2014-06-20T09:14:07+00:00");
		assertThat(map.get(SyslogHeaders.HOST)).isEqualTo("loggregator");
		assertThat(map.get(SyslogHeaders.APP_NAME)).isEqualTo("d0602076-b14a-4c55-852a-981e7afeed38");
		assertThat(map.get(SyslogHeaders.PROCID)).isEqualTo("DEA");
		assertThat(map.get(SyslogHeaders.MSGID)).isEqualTo("-");
		assertThat(map.get(SyslogHeaders.MESSAGE)).isEqualTo("Removing instance");
	}

	@Test
	public void shouldGetStructuredData() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(SD_ENTRY_1.getBytes()));

		assertThat(map.get(SyslogHeaders.FACILITY)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.SEVERITY)).isEqualTo(6);
		assertThat(map.get(SyslogHeaders.VERSION)).isEqualTo(1);
		assertThat(map.get(SyslogHeaders.TIMESTAMP)).isEqualTo("2014-06-20T09:14:07+00:00");
		assertThat(map.get(SyslogHeaders.HOST)).isEqualTo("loggregator");
		assertThat(map.get(SyslogHeaders.APP_NAME)).isEqualTo("d0602076-b14a-4c55-852a-981e7afeed38");
		assertThat(map.get(SyslogHeaders.PROCID)).isEqualTo("DEA");
		assertThat(map.get(SyslogHeaders.MSGID)).isEqualTo("-");
		assertThat(map.get(SyslogHeaders.MESSAGE)).isEqualTo("Removing instance");
		assertThat(((List<?>) map.get(SyslogHeaders.STRUCTURED_DATA)).size()).isEqualTo(1);
	}

	@Test
	public void shouldGetMultipleStructuredData() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(SD_ENTRY_2.getBytes()));

		assertThat(map.get(SyslogHeaders.DECODE_ERRORS)).isEqualTo("false");
		assertThat(map.get(SyslogHeaders.MESSAGE)).isEqualTo("Removing instance");
		assertThat(((List<?>) map.get(SyslogHeaders.STRUCTURED_DATA)).size()).isEqualTo(2);
	}

	@Test
	public void shouldGetMultipleStructuredDataWithEscapedBracket() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(SD_ENTRY_3.getBytes()));

		assertThat(map.get(SyslogHeaders.DECODE_ERRORS)).isEqualTo("false");
		assertThat(map.get(SyslogHeaders.MESSAGE)).isEqualTo("Removing instance");
	}

	@Test
	public void shouldErrorOnShortFramedData() throws Exception {

		RFC6587SyslogDeserializer deserializer = new RFC6587SyslogDeserializer();

		Map<String, ?> map = deserializer.deserialize(new ByteArrayInputStream(SHORT_FRAMED_ENTRY.getBytes()));

		assertThat(map.get(SyslogHeaders.DECODE_ERRORS)).isEqualTo("true");
	}

}
