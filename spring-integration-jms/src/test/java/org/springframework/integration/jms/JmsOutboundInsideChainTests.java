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

package org.springframework.integration.jms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * //INT-2275
 *
 * @author Artem Bilan
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class JmsOutboundInsideChainTests {

	@Autowired
	private MessageChannel outboundChainChannel;

	@Autowired
	private PollableChannel receiveChannel;

	@Autowired
	private MessageChannel outboundGatewayChainChannel;

	@Autowired
	private PollableChannel repliesChannel;

	@Test
	public void testJmsOutboundChannelInsideChain(){
		String testString = "test";
		Message<String> shippedMessage = MessageBuilder.withPayload(testString).build();
		this.outboundChainChannel.send(shippedMessage);
		Message<?> receivedMessage = this.receiveChannel.receive(2000);
		assertEquals(testString, receivedMessage.getPayload());
	}

	@Test
	public void testJmsOutboundGatewayRequiresReply(){
		this.outboundGatewayChainChannel.send(MessageBuilder.withPayload("test").build());
		assertNotNull(this.repliesChannel.receive(2000));

		this.outboundGatewayChainChannel.send(MessageBuilder.withPayload("test").build());
		assertNull(this.repliesChannel.receive(2000));
	}

}
