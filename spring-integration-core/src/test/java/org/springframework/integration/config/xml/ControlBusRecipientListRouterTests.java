/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
/**
 * @author Liujiong
 * @since 4.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ControlBusRecipientListRouterTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	@Qualifier("routingChannelA")
	private MessageChannel channel;

	@Test
	public void testAddRecipient() {
		context.start();
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel2','true')");
		Message<?>  message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive(0).getPayload().equals(1));
		assertTrue(chanel2.receive(0).getPayload().equals(1));
	}

	@Test
	public void testAddRecipientWithNullExpression() {
		context.start();
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.addRecipient('channel2')");

		Message<?>  message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive(0).getPayload().equals(1));
		assertTrue(chanel2.receive(0).getPayload().equals(1));
	}
	
	@Test
	public void testRemoveRecipient() {
		context.start();
		MessagingTemplate messagingTemplate = new MessagingTemplate();
		messagingTemplate.setReceiveTimeout(1000);
		messagingTemplate.convertAndSend(input, "@'simpleRouter.handler'.removeRecipient('channel2')");

		Message<?>  message = new GenericMessage<Integer>(1);
		channel.send(message);
		PollableChannel chanel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel chanel2 = (PollableChannel) context.getBean("channel2");
		assertTrue(chanel1.receive(0).getPayload().equals(1));
		assertNull(chanel2.receive(0));
	}
}
