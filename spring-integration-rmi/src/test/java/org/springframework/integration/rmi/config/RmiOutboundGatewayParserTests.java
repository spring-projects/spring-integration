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

package org.springframework.integration.rmi.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.rmi.RmiInboundGateway;
import org.springframework.integration.rmi.RmiOutboundGateway;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class RmiOutboundGatewayParserTests {

	private final QueueChannel testChannel = new QueueChannel();

	private static volatile int adviceCalled;

	@Before
	public void setupTestInboundGateway() throws Exception {
		testChannel.setBeanName("testChannel");
		RmiInboundGateway gateway = new RmiInboundGateway();
		gateway.setRequestChannel(testChannel);
		gateway.setExpectReply(false);
		gateway.setBeanFactory(mock(BeanFactory.class));
		gateway.afterPropertiesSet();
	}

	@Test
	public void testOrder() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		RmiOutboundGateway gateway = context.getBean("gateway.handler", RmiOutboundGateway.class);
		assertEquals(23, TestUtils.getPropertyValue(gateway, "order"));
		assertTrue(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class));
		context.close();
	}

	@Test
	public void directInvocation() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel localChannel = (MessageChannel) context.getBean("advisedChannel");
		RmiOutboundGateway gateway = context.getBean("advised.handler", RmiOutboundGateway.class);
		assertFalse(TestUtils.getPropertyValue(gateway, "requiresReply", Boolean.class));

		localChannel.send(new GenericMessage<String>("test"));
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
		assertEquals(1, adviceCalled);
		context.close();
	}

	@Test //INT-1029
	public void testRmiOutboundGatewayInsideChain() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel localChannel = context.getBean("rmiOutboundGatewayInsideChain", MessageChannel.class);
		localChannel.send(MessageBuilder.withPayload("test").build());
		Message<?> result = testChannel.receive(1000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
		context.close();
	}

	@Test //INT-1029
	public void testRmiRequestReplyWithinChain() {
		ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"rmiOutboundGatewayParserTests.xml", this.getClass());
		MessageChannel localChannel = context.getBean("requestReplyRmiWithChainChannel", MessageChannel.class);
		localChannel.send(MessageBuilder.withPayload("test").build());
		PollableChannel replyChannel = context.getBean("replyChannel", PollableChannel.class);
		Message<?> result = replyChannel.receive();
		assertNotNull(result);
		assertEquals("TEST", result.getPayload());
		context.close();
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return callback.execute();
		}

	}
}
