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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * See INT-1688 for background.
 * 
 * @author Mark Fisher
 * @since 2.0.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ServiceActivatorDefaultFrameworkMethodTests {

	@Autowired
	private MessageChannel gatewayTestInputChannel;

	@Autowired
	private MessageChannel handlerTestInputChannel;

	@Test
	public void testGateway() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.gatewayTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("gatewayTestInputChannel,gatewayTestService,gateway,requestChannel,bridge,replyChannel", reply.getHeaders().get("history").toString());
	}

	@Test
	public void testMessageHandler() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload("test").setReplyChannel(replyChannel).build();
		this.handlerTestInputChannel.send(message);
		Message<?> reply = replyChannel.receive(0);
		assertEquals("TEST", reply.getPayload());
		assertEquals("handlerTestInputChannel,handlerTestService,testMessageHandler", reply.getHeaders().get("history").toString());
	}


	@SuppressWarnings("unused")
	private static class TestMessageHandler extends AbstractReplyProducingMessageHandler {

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage.getPayload().toString().toUpperCase();
		}
	}

}
