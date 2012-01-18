/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.jmx.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import javax.management.Notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.jmx.NotificationListeningMessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NotificationListeningChannelAdapterParserTests {

	@Autowired
	private PollableChannel channel;

	@Autowired
	private TestPublisher testPublisher;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private NotificationListeningMessageProducer autoChannelAdapter;

	@Test
	public void receiveNotification() throws Exception {
		assertNull(channel.receive(0));
		testPublisher.send("ABC");
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals(Notification.class, message.getPayload().getClass());
		assertEquals("ABC", ((Notification) message.getPayload()).getMessage());
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}

}
