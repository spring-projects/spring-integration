/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class DispatcherHasNoSubscribersTests {

	@Autowired
	MessageChannel noSubscribersChannel;

	@Autowired
	MessageChannel subscribedChannel;

	@Autowired
	AbstractApplicationContext applicationContext;

	@BeforeEach
	public void setup() {
		applicationContext.setId("testApplicationId");
	}

	@Test
	public void oneChannel() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> noSubscribersChannel.send(new GenericMessage<>("Hello, world!")))
				.withMessageContaining("Dispatcher has no subscribers for channel 'testApplicationId.noSubscribersChannel'.");
	}

	@Test
	public void stackedChannels() {
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> subscribedChannel.send(new GenericMessage<>("Hello, world!")))
				.withMessageContaining("Dispatcher has no subscribers for channel 'testApplicationId.noSubscribersChannel'.");
	}

	@Test
	public void withNoContext() {
		DirectChannel channel = new DirectChannel();
		channel.setBeanName("testChannel");
		assertThatExceptionOfType(MessagingException.class)
				.isThrownBy(() -> channel.send(new GenericMessage<String>("Hello, world!")))
				.withMessageContaining("Dispatcher has no subscribers for channel 'testChannel'.");
	}

}
