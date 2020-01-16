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

package org.springframework.integration.test.matcher;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 */
public class HeaderMatcherTests {

	static final String UNKNOWN_KEY = "unknownKey";

	static final String ANY_HEADER_VALUE = "bar";

	static final String ANY_HEADER_KEY = "test.foo";

	static final String ANY_PAYLOAD = "bla";

	static final String OTHER_HEADER_KEY = "test.number";

	static final Integer OTHER_HEADER_VALUE = 123;

	Message<?> message;

	@BeforeEach
	public void setUp() {
		message = MessageBuilder.withPayload(ANY_PAYLOAD)
				.setHeader(ANY_HEADER_KEY, ANY_HEADER_VALUE)
				.setHeader(OTHER_HEADER_KEY, OTHER_HEADER_VALUE).build();
	}

	@Test
	public void hasEntry_withValidKeyValue_matches() {
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeader(ANY_HEADER_KEY, ANY_HEADER_VALUE));
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeader(OTHER_HEADER_KEY, OTHER_HEADER_VALUE));
	}

	@Test
	public void hasEntry_withUnknownKey_notMatching() {
		MatcherAssert.assertThat(message, Matchers.not(HeaderMatcher.hasHeader("test.unknown", ANY_HEADER_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_matches() {
		MatcherAssert.assertThat(message,
				HeaderMatcher.hasHeader(ANY_HEADER_KEY, Matchers.instanceOf(String.class)));
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeader(ANY_HEADER_KEY, Matchers.notNullValue()));
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeader(ANY_HEADER_KEY, Matchers.is(ANY_HEADER_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_notMatching() {
		MatcherAssert.assertThat(message,
				Matchers.not(HeaderMatcher.hasHeader(ANY_HEADER_KEY,
						Matchers.is(Matchers.instanceOf(Integer.class)))));
	}

	@Test
	public void hasKey_withValidKey_matches() {
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeaderKey(ANY_HEADER_KEY));
		MatcherAssert.assertThat(message, HeaderMatcher.hasHeaderKey(OTHER_HEADER_KEY));
	}

	@Test
	public void hasKey_withInvalidKey_notMatching() {
		MatcherAssert.assertThat(message, Matchers.not(HeaderMatcher.hasHeaderKey(UNKNOWN_KEY)));
	}

	@Test
	public void hasAllEntries_withMessageHeader_matches() {
		Map<String, Object> expectedInHeaderMap = message.getHeaders();
		MatcherAssert.assertThat(message, HeaderMatcher.hasAllHeaders(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withValidKeyValueOrMatcherValue_matches() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE);
		expectedInHeaderMap.put(OTHER_HEADER_KEY, Matchers.is(OTHER_HEADER_VALUE));
		MatcherAssert.assertThat(message, HeaderMatcher.hasAllHeaders(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withInvalidValidKeyValueOrMatcherValue_notMatching() {
		Map<String, Object> expectedInHeaderMap = new HashMap<>();
		expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE); // valid
		expectedInHeaderMap.put(UNKNOWN_KEY, Matchers.not(Matchers.nullValue())); // fails
		MatcherAssert.assertThat(message, Matchers.not(HeaderMatcher.hasAllHeaders(expectedInHeaderMap)));
		expectedInHeaderMap.remove(UNKNOWN_KEY);
		expectedInHeaderMap.put(OTHER_HEADER_KEY, ANY_HEADER_VALUE); // fails
	}

	@Test
	public void readableException_singleHeader() {
		try {
			MatcherAssert.assertThat(message, HeaderMatcher.hasHeader("corn", "bread"));
		}
		catch (AssertionError ae) {
			MatcherAssert.assertThat(ae.getMessage(),
					Matchers.containsString("Expected: a Message with Headers containing "));
		}
	}

	@Test
	public void readableException_allHeaders() {
		try {
			Map<String, String> entries = new HashMap<>();
			entries.put("corn", "bread");
			entries.put("chocolate", "pudding");
			MatcherAssert.assertThat(message, HeaderMatcher.hasAllHeaders(entries));
		}
		catch (AssertionError ae) {
			MatcherAssert.assertThat(ae.getMessage(),
					Matchers.containsString("Expected: a Message with Headers containing "));
		}
	}

	@Test
	public void hasMessageId_sameId() {
		MatcherAssert.assertThat(message, HeaderMatcher.hasMessageId(message.getHeaders().getId()));
	}

	@Test
	public void hasCorrelationId_() {
		UUID correlationId = message.getHeaders().getId();
		message = MessageBuilder.withPayload("blabla").setHeader("correlationId", correlationId).build();
		MatcherAssert.assertThat(message, HeaderMatcher.hasCorrelationId(correlationId));
	}

	@Test
	public void hasSequenceNumber_() {
		int sequenceNumber = 123;
		message = MessageBuilder.fromMessage(message).setHeader("sequenceNumber", sequenceNumber).build();
		MatcherAssert.assertThat(message, HeaderMatcher.hasSequenceNumber(sequenceNumber));
	}

	@Test
	public void hasSequenceSize_() {
		int sequenceSize = 123;
		message = MessageBuilder.fromMessage(message).setHeader("sequenceSize", sequenceSize).build();
		MatcherAssert.assertThat(message, HeaderMatcher.hasSequenceSize(sequenceSize));
		MatcherAssert.assertThat(message, HeaderMatcher.hasSequenceSize(Matchers.is(sequenceSize)));
	}

	@Test
	public void hasTimestamp_() {
		MatcherAssert.assertThat(message, HeaderMatcher.hasTimestamp(new Date(message.getHeaders().getTimestamp())));
	}

	@Test
	public void hasExpirationDate_() {
		MatcherAssert.assertThat(message, Matchers.not(HeaderMatcher.hasExpirationDate(Matchers.any(Long.class))));
		Date expirationDate = new Date(System.currentTimeMillis() + 10000);
		message = MessageBuilder.fromMessage(message).setHeader("expirationDate", expirationDate.getTime()).build();
		MatcherAssert.assertThat(message, HeaderMatcher.hasExpirationDate(expirationDate));
		MatcherAssert.assertThat(message,
				HeaderMatcher.hasExpirationDate(Matchers.not(Matchers.is((System.currentTimeMillis())))));
	}

}
