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

import javax.management.Notification;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.jmx.JmxHeaders;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class NotificationPublishingChannelAdapterParserTests {

	@Autowired
	private MessageChannel channel;

	@Autowired
	private TestListener listener;

	@Test
	public void publishAndVerify() throws Exception {
		assertNull(listener.lastNotification);
		Message<?> message = MessageBuilder.withPayload("XYZ")
				.setHeader(JmxHeaders.NOTIFICATION_TYPE, "test.type").build();
		channel.send(message);
		assertNotNull(listener.lastNotification);
		Notification notification = listener.lastNotification;
		assertEquals("XYZ", (notification).getMessage());
		assertEquals("test.type", notification.getType());
	}

}
