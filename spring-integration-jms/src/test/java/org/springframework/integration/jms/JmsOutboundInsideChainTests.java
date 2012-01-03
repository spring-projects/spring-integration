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

import org.junit.Test;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;

import static org.junit.Assert.*;

/**
 * //INT-2275
 *
 * @author Artem Bilan
 */
public class JmsOutboundInsideChainTests {

	@Test
	public void testJmsOutboundChannelInsideChain(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("JmsOutboundInsideChainTests-context.xml", getClass());
		PollableChannel receiveChannel = context.getBean("receiveChannel", PollableChannel.class);
		MessageChannel outboundChainChannel = context.getBean("outboundChainChannel", MessageChannel.class);
		String testString = "test";
		Message<String> shippedMessage = MessageBuilder.withPayload(testString).build();
		outboundChainChannel.send(shippedMessage);
		Message<?> receivedMessage = receiveChannel.receive();
		assertEquals(testString, receivedMessage.getPayload());
		context.close();
	}

}
