/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.redis.channel.registry;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gunnar Hillert
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode=ClassMode.AFTER_EACH_TEST_METHOD)
public class RedisChannelRegistryTests extends RedisAvailableTests {

	@Autowired
	private RedisChannelRegistry registry;

	@Test
	@RedisAvailable
	public void testOutboungToInbound() throws InterruptedException {

		final DirectChannel outboundChannel = new DirectChannel();
		registry.outbound("outboundToInbound", outboundChannel);

		final DirectChannel inboundChannel = new DirectChannel();
		registry.inbound("outboundToInbound", inboundChannel);

		final CountDownLatch messageReceived = new CountDownLatch(1);
		inboundChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				messageReceived.countDown();
				assertEquals("hello", message.getPayload());
			}
		});

		outboundChannel.send(new GenericMessage<String>("hello"));
		assertTrue(messageReceived.await(5000, TimeUnit.MILLISECONDS));
	}

}
