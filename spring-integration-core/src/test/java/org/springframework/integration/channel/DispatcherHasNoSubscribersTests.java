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
package org.springframework.integration.channel;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

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

/**
 * @author Gary Russell
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
		} catch (MessagingException e) {
			assertEquals("Dispatcher has no subscribers for channel 'foo.noSubscribersChannel'.", e.getMessage());
		}
	}

	@Test
	public void stackedChannels() {
		try {
			subscribedChannel.send(new GenericMessage<String>("Hello, world!"));
			fail("Exception expected");
		} catch (MessagingException e) {
			assertEquals("Dispatcher has no subscribers for channel 'foo.noSubscribersChannel'.", e.getMessage());
		}
	}

	@Test
	public void withNoContext() {
		DirectChannel channel = new DirectChannel();
		channel.setBeanName("bar");
		try {
			channel.send(new GenericMessage<String>("Hello, world!"));
			fail("Exception expected");
		} catch (MessagingException e) {
			assertEquals("Dispatcher has no subscribers for channel 'bar'.", e.getMessage());
		}
	}

}
