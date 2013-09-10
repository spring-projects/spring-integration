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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.integration.test.matcher.PayloadMatcher.hasPayload;

import java.math.BigDecimal;

import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Alex Peters
 * @author Iwein Fuld
 * @author Gunnar Hillert
 */
public class PayloadMatcherTests {

	static final BigDecimal ANY_PAYLOAD = new BigDecimal("1.123");

	Message<BigDecimal> message = MessageBuilder.withPayload(ANY_PAYLOAD).build();

	@Test
	public void hasPayload_withEqualValue_matches() throws Exception {
		assertThat(message, hasPayload(new BigDecimal("1.123")));
	}

	@Test
	public void hasPayload_withNotEqualValue_notMatching() throws Exception {
		assertThat(message, not(hasPayload(new BigDecimal("456"))));
	}

	@Test
	public void hasPayload_withMatcher_matches() throws Exception {
		assertThat(message,
				hasPayload(is(instanceOf(BigDecimal.class))));
		assertThat(message, hasPayload(notNullValue()));
	}

	@Test
	public void hasPayload_withNotMatchingMatcher_notMatching()
			throws Exception {
		assertThat(message, not((hasPayload(is(instanceOf(String.class))))));
	}

	@Test
	public void readableException() throws Exception {
		try {
			assertThat(message, hasPayload("woot"));
		} catch(AssertionError ae){
			assertTrue(ae.getMessage().contains("Expected: a Message with payload: "));
		}
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void shouldMatchNonParametrizedMessage() throws Exception {
		Message message = this.message;
		assertThat(message, hasPayload(new BigDecimal("1.123")));
	}

}
