/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.test.matcher;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;

import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasAllHeaders;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasCorrelationId;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasExpirationDate;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeader;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasHeaderKey;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasMessageId;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasSequenceNumber;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasSequenceSize;
import static org.springframework.integration.test.matcher.HeaderMatcher.hasTimestamp;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 *
 */
public class HeaderMatcherTests {

	static final String UNKNOWN_KEY = "unknownKey";

	static final String ANY_HEADER_VALUE = "bar";

	static final String ANY_HEADER_KEY = "test.foo";

	static final String ANY_PAYLOAD = "bla";

	static final String OTHER_HEADER_KEY = "test.number";

	static final Integer OTHER_HEADER_VALUE = Integer.valueOf(123);

	Message<?> message;

	@Before
	public void setUp() {
		message = MessageBuilder.withPayload(ANY_PAYLOAD).setHeader(ANY_HEADER_KEY, ANY_HEADER_VALUE).setHeader(
				OTHER_HEADER_KEY, OTHER_HEADER_VALUE).build();
	}

	@Test
	public void hasEntry_withValidKeyValue_matches() throws Exception {
		assertThat(message, hasHeader(ANY_HEADER_KEY, ANY_HEADER_VALUE));
		assertThat(message, hasHeader(OTHER_HEADER_KEY, OTHER_HEADER_VALUE));
	}

	@Test
	public void hasEntry_withUnknownKey_notMatching() throws Exception {
		assertThat(message, not(hasHeader("test.unknown", ANY_HEADER_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_matches() throws Exception {
		assertThat(message, hasHeader(ANY_HEADER_KEY, is(instanceOf(String.class))));
		assertThat(message, hasHeader(ANY_HEADER_KEY, notNullValue()));
		assertThat(message, hasHeader(ANY_HEADER_KEY, is(ANY_HEADER_VALUE)));
	}

	@Test
	public void hasEntry_withValidKeyAndMatcherValue_notMatching() throws Exception {
		assertThat(message, not(hasHeader(ANY_HEADER_KEY, is(instanceOf(Integer.class)))));
	}

	@Test
	public void hasKey_withValidKey_matches() throws Exception {
		assertThat(message, hasHeaderKey(ANY_HEADER_KEY));
		assertThat(message, hasHeaderKey(OTHER_HEADER_KEY));
	}

	@Test
	public void hasKey_withInvalidKey_notMatching() throws Exception {
		assertThat(message, not(hasHeaderKey(UNKNOWN_KEY)));
	}

	@Test
	public void hasAllEntries_withMessageHeader_matches() throws Exception {
		Map<String, Object> expectedInHeaderMap = message.getHeaders();
		assertThat(message, hasAllHeaders(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withValidKeyValueOrMatcherValue_matches() throws Exception {
		Map<String, Object> expectedInHeaderMap = new HashMap<String, Object>();
		expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE);
		expectedInHeaderMap.put(OTHER_HEADER_KEY, is(OTHER_HEADER_VALUE));
		assertThat(message, hasAllHeaders(expectedInHeaderMap));
	}

	@Test
	public void hasAllEntries_withInvalidValidKeyValueOrMatcherValue_notMatching() throws Exception {
		Map<String, Object> expectedInHeaderMap = new HashMap<String, Object>();
		expectedInHeaderMap.put(ANY_HEADER_KEY, ANY_HEADER_VALUE); // valid
		expectedInHeaderMap.put(UNKNOWN_KEY, not(nullValue())); // fails
		assertThat(message, not(hasAllHeaders(expectedInHeaderMap)));
		expectedInHeaderMap.remove(UNKNOWN_KEY);
		expectedInHeaderMap.put(OTHER_HEADER_KEY, ANY_HEADER_VALUE); // fails
	}

	@Test
	public void readableException_singleHeader() throws Exception {
		try {
			assertThat(message, hasHeader("corn", "bread"));
		}
		catch (AssertionError ae) {
			assertTrue(ae.getMessage().contains("Expected: a Message with Headers containing "));
		}
	}

	@Test
	public void readableException_allHeaders() throws Exception {
		try {
			Map<String, String> entries = new HashMap<String, String>();
			entries.put("corn", "bread");
			entries.put("chocolate", "pudding");
			assertThat(message, hasAllHeaders(entries));
		}
		catch (AssertionError ae) {
			assertTrue(ae.getMessage().contains("Expected: a Message with Headers containing "));
		}
	}

	@Test
	public void hasMessageId_sameId() throws Exception {
		assertThat(message, hasMessageId(message.getHeaders().getId()));
	}

	@Test
	public void hasCorrelationId_() throws Exception {
		UUID correlationId = message.getHeaders().getId();
		message = MessageBuilder.withPayload("blabla").setCorrelationId(correlationId).build();
		assertThat(message, hasCorrelationId(correlationId));
	}

	@Test
	public void hasSequenceNumber_() throws Exception {
		int sequenceNumber = 123;
		message = MessageBuilder.fromMessage(message).setSequenceNumber(sequenceNumber).build();
		assertThat(message, hasSequenceNumber(sequenceNumber));
	}

	@Test
	public void hasSequenceSize_() throws Exception {
		int sequenceSize = 123;
		message = MessageBuilder.fromMessage(message).setSequenceSize(sequenceSize).build();
		assertThat(message, hasSequenceSize(sequenceSize));
		assertThat(message, hasSequenceSize(is(sequenceSize)));
	}

	@Test
	public void hasTimestamp_() throws Exception {
		assertThat(message, hasTimestamp(new Date(message.getHeaders().getTimestamp())));
	}

	@Test
	public void hasExpirationDate_() throws Exception {
		Matcher<Long> anyMatcher = any(Long.class);

		assertThat(message, not(hasExpirationDate(anyMatcher)));
		Date expirationDate = new Date(System.currentTimeMillis() + 10000);
		message = MessageBuilder.fromMessage(message).setExpirationDate(expirationDate).build();
		assertThat(message, hasExpirationDate(expirationDate));
		assertThat(message, hasExpirationDate(not(is((System.currentTimeMillis())))));
	}

}
