/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.gateway.config;

import static org.junit.Assert.assertEquals;

import java.util.concurrent.Executors;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.gateway.TestService;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class GatewayParserTests {

	@Test
	public void testOneWay() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		TestService service = (TestService) context.getBean("oneWay");
		service.oneWay("foo");
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		Message<?> result = channel.receive(1000);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void testSolicitResponse() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("replyChannel");
		channel.send(new StringMessage("foo"));
		TestService service = (TestService) context.getBean("solicitResponse");
		String result = service.solicitResponse();
		assertEquals("foo", result);
	}

	@Test
	public void testRequestReply() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReply");
		String result = service.requestReply("foo");
		assertEquals("foo", result);		
	}

	@Test
	public void testRequestReplyWithMessageMapper() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReplyWithMessageMapper");
		String result = service.requestReply("foo");
		assertEquals("foo.mapped", result);		
	}

	@Test
	public void testRequestReplyWithMessageCreator() {
		ApplicationContext context = new ClassPathXmlApplicationContext("gatewayParserTests.xml", this.getClass());
		PollableChannel requestChannel = (PollableChannel) context.getBean("requestChannel");
		MessageChannel replyChannel = (MessageChannel) context.getBean("replyChannel");
		this.startResponder(requestChannel, replyChannel);
		TestService service = (TestService) context.getBean("requestReplyWithMessageCreator");
		String result = service.requestReply("foo");
		assertEquals("created.foo", result);		
	}


	private void startResponder(final PollableChannel requestChannel, final MessageChannel replyChannel) {
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				Message<?> request = requestChannel.receive();
				Message<?> reply = MessageBuilder.fromMessage(request)
						.setCorrelationId(request.getHeaders().getId()).build();
				replyChannel.send(reply);
			}
		});
	}

}
