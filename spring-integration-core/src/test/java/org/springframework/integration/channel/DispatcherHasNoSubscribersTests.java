/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.channel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class DispatcherHasNoSubscribersTests {

	@Autowired
	MessageChannel noSubscribersChannel;

	@Autowired
	MessageChannel subscribedChannel;

	@Autowired
	AbstractApplicationContext applicationContext;

	@Before
	public void setup() {
		applicationContext.setId("foo");
	}

	@Test
	public void oneChannel() {
		try {
			noSubscribersChannel.send(new GenericMessage<String>("Hello, world!"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage())
					.contains("Dispatcher has no subscribers for channel 'foo.noSubscribersChannel'.");
		}
	}

	@Test
	public void stackedChannels() {
		try {
			subscribedChannel.send(new GenericMessage<String>("Hello, world!"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage())
					.contains("Dispatcher has no subscribers for channel 'foo.noSubscribersChannel'.");
		}
	}

	@Test
	public void withNoContext() {
		DirectChannel channel = new DirectChannel();
		channel.setBeanName("bar");
		try {
			channel.send(new GenericMessage<String>("Hello, world!"));
			fail("Exception expected");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage()).contains("Dispatcher has no subscribers for channel 'bar'.");
		}
	}

}
