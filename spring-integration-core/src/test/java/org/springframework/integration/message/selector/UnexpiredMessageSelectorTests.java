/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.message.selector;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.selector.UnexpiredMessageSelector;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class UnexpiredMessageSelectorTests {

	@Test
	public void testExpiredMessageRejected() {
		long past = System.currentTimeMillis() - 60000;
		Message<String> message = MessageBuilder.withPayload("expired")
				.setExpirationDate(past).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertFalse(selector.accept(message));
	}

	@Test
	public void testUnexpiredMessageAccepted() {
		long future = System.currentTimeMillis() + 60000;
		Message<String> message = MessageBuilder.withPayload("unexpired")
				.setExpirationDate(future).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertTrue(selector.accept(message));
	}

	@Test
	public void testMessageWithNullExpirationDateNeverExpires() {
		Message<String> message = MessageBuilder.withPayload("unexpired").build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertTrue(selector.accept(message));
	}

}
