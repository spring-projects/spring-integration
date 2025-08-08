/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
