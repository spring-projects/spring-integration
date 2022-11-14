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

import java.util.UUID;

import org.junit.Test;

import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ClaimCheckTransformerTests {

	@Test
	public void store() {
		MessageStore store = new SimpleMessageStore(10);
		ClaimCheckInTransformer transformer = new ClaimCheckInTransformer(store);
		Message<?> input = MessageBuilder.withPayload("test").build();
		Message<?> output = transformer.transform(input);
		assertThat(output.getPayload()).isEqualTo(input.getHeaders().getId());
	}

	@Test
	public void retrieve() {
		MessageStore store = new SimpleMessageStore(10);
		Message<?> message = MessageBuilder.withPayload("test").build();
		UUID storedId = message.getHeaders().getId();
		store.addMessage(message);
		ClaimCheckOutTransformer transformer = new ClaimCheckOutTransformer(store);
		Message<?> input = MessageBuilder.withPayload(storedId).build();
		Message<?> output = transformer.transform(input);
		assertThat(output.getPayload()).isEqualTo("test");
	}

	@Test(expected = MessageTransformationException.class)
	public void unknown() {
		MessageStore store = new SimpleMessageStore(10);
		ClaimCheckOutTransformer transformer = new ClaimCheckOutTransformer(store);
		transformer.transform(MessageBuilder.withPayload(UUID.randomUUID()).build());
	}

}
