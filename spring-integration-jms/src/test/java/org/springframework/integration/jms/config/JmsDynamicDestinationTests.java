/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsDynamicDestinationTests {

	@Autowired
	private MessageChannel channelAdapterChannel;

	@Autowired
	private MessageChannel gatewayChannel;

	@Autowired
	private PollableChannel channelAdapterResults1;

	@Autowired
	private PollableChannel channelAdapterResults2;


	@Before
	public void prepareActiveMq() {
		ActiveMqTestUtils.prepare();
	}

	@Test
	public void channelAdapter() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test-1").setHeader("destinationNumber", 1).build();
		Message<?> message2 = MessageBuilder.withPayload("test-2").setHeader("destinationNumber", 2).build();
		channelAdapterChannel.send(message1);
		channelAdapterChannel.send(message2);
		Message<?> result1 = channelAdapterResults1.receive(5000);
		Message<?> result2 = channelAdapterResults2.receive(5000);
		assertNotNull(result1);
		assertNotNull(result2);
		TextMessage jmsResult1 = (TextMessage) result1.getPayload();
		TextMessage jmsResult2 = (TextMessage) result2.getPayload();
		assertEquals("test-1", jmsResult1.getText());
		assertEquals("queue://queue.test.dynamic.adapter.1", jmsResult1.getJMSDestination().toString());
		assertEquals("test-2", jmsResult2.getText());
		assertEquals("queue://queue.test.dynamic.adapter.2", jmsResult2.getJMSDestination().toString());
	}

	@Test
	public void gateway() throws Exception {
		Message<?> message1 = MessageBuilder.withPayload("test-1").setHeader("destinationNumber", 1).build();
		Message<?> message2 = MessageBuilder.withPayload("test-2").setHeader("destinationNumber", 2).build();
		MessagingTemplate template = new MessagingTemplate();
		Message<?> result1 = template.sendAndReceive(gatewayChannel, message1);
		Message<?> result2 = template.sendAndReceive(gatewayChannel, message2);
		assertNotNull(result1);
		assertNotNull(result2);
		assertEquals("test-1!", result1.getPayload());
		assertEquals("test-2!!", result2.getPayload());
	}


	public static class Responder {

		public String one(String message) {
			return message + "!";
		}

		public String two(String message) {
			return message + "!!";
		}
	}

}
