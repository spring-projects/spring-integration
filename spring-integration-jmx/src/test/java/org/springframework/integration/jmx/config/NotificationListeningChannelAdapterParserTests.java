/*
 * Copyright 2002-2012 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import javax.management.Notification;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.jmx.NotificationListeningMessageProducer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NotificationListeningChannelAdapterParserTests {

	@Autowired
	private PollableChannel channel;

	@Autowired
	private PollableChannel patternChannel;

	@Autowired
	private PollableChannel multiChannel;

	@Autowired
	private TestPublisher testPublisher;

	@Autowired
	private TestPublisher testPublisher2;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired @Qualifier("autoChannel.adapter")
	private NotificationListeningMessageProducer autoChannelAdapter;

	@Autowired @Qualifier("multiAdapter")
	private NotificationListeningMessageProducer multiAdapter;

	@Test
	public void receiveNotification() throws Exception {
		this.multiAdapter.start();
		assertNull(channel.receive(0));
		testPublisher.send("ABC");
		verifyReceipt(channel, "testPublisher");
		verifyReceipt(patternChannel, "testPublisher");
		// multiChannel should see 2 copies
		verifyReceipt(multiChannel, "testPublisher");
		verifyReceipt(multiChannel, "testPublisher");

		testPublisher2.send("ABC");
		assertNull(channel.receive(0));
		assertNull(patternChannel.receive(0));
		// multiChannel should see only 1 copy
		verifyReceipt(multiChannel, "testPublisher2");
		assertNull(multiChannel.receive(0));
	}

	private void verifyReceipt(PollableChannel channel, String beanName) {
		Message<?> message = channel.receive(1000);
		assertNotNull(message);
		assertEquals(Notification.class, message.getPayload().getClass());
		assertEquals("ABC", ((Notification) message.getPayload()).getMessage());
		assertTrue(((String) ((Notification) message.getPayload()).getSource()).endsWith(beanName));
	}

	@Test
	public void testAutoChannel() {
		assertSame(autoChannel, TestUtils.getPropertyValue(autoChannelAdapter, "outputChannel"));
	}

}
