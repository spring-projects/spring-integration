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
import static org.junit.Assert.assertTrue;

import javax.jms.JMSException;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.jms.TextMessage;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.listener.SessionAwareMessageListener;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsPriorityTests {

	@Autowired
	private MessageChannel channelAdapterChannel;

	@Autowired
	private PollableChannel channelAdapterResults;

	@Autowired
	private MessageChannel gatewayChannel;


	@Before
	public void prepareActiveMq() {
		ActiveMqTestUtils.prepare();
	}

	@Test
	public void verifyPrioritySettingOnChannelAdapterUsedAsJmsPriorityIfNoHeader() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").build();
		channelAdapterChannel.send(message);
		Message<?> result = channelAdapterResults.receive(5000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof javax.jms.Message);
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertEquals(3, jmsMessage.getJMSPriority());
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriorityWithChannelAdapter() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").setPriority(7).build();
		channelAdapterChannel.send(message);
		Message<?> result = channelAdapterResults.receive(5000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof javax.jms.Message);
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertEquals(7, jmsMessage.getJMSPriority());
	}

	@Test
	public void verifyPrioritySettingOnGatewayUsedAsJmsPriorityIfNoHeader() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		gatewayChannel.send(message);
		Message<?> result = replyChannel.receive(5000);
		assertNotNull(result);
		assertEquals("priority=2", result.getPayload());
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriorityWithGateway() throws Exception {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setPriority(8).setReplyChannel(replyChannel).build();
		gatewayChannel.send(message);
		Message<?> result = replyChannel.receive(5000);
		assertNotNull(result);
		assertEquals("priority=8", result.getPayload());
	}


	public static class PriorityReader implements SessionAwareMessageListener<javax.jms.Message> {

		public void onMessage(javax.jms.Message request, Session session) throws JMSException {
			String text = "priority=" + request.getJMSPriority();
			TextMessage reply = session.createTextMessage(text);
			MessageProducer producer = session.createProducer(request.getJMSReplyTo());
			reply.setJMSCorrelationID(request.getJMSMessageID());
			producer.send(reply);
		}
	}

}
