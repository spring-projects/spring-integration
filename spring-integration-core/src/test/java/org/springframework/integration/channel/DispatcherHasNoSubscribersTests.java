/*
 * Copyright 2002-2024 the original author or authors.
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
