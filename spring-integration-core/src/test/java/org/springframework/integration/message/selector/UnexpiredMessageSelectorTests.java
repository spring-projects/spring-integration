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

package org.springframework.integration.message.selector;

import org.junit.Test;

import org.springframework.integration.selector.UnexpiredMessageSelector;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class UnexpiredMessageSelectorTests {

	@Test
	public void testExpiredMessageRejected() {
		long past = System.currentTimeMillis() - 60000;
		Message<String> message = MessageBuilder.withPayload("expired")
				.setExpirationDate(past).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isFalse();
	}

	@Test
	public void testUnexpiredMessageAccepted() {
		long future = System.currentTimeMillis() + 60000;
		Message<String> message = MessageBuilder.withPayload("unexpired")
				.setExpirationDate(future).build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isTrue();
	}

	@Test
	public void testMessageWithNullExpirationDateNeverExpires() {
		Message<String> message = MessageBuilder.withPayload("unexpired").build();
		UnexpiredMessageSelector selector = new UnexpiredMessageSelector();
		assertThat(selector.accept(message)).isTrue();
	}

}
