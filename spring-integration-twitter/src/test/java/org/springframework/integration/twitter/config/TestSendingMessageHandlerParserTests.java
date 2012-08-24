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

package org.springframework.integration.twitter.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.Test;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.twitter.outbound.DirectMessageSendingMessageHandler;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class TestSendingMessageHandlerParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testSendingMessageHandlerSuccessfulBootstrap(){
		ApplicationContext ac = new ClassPathXmlApplicationContext("TestSendingMessageHandlerParser-context.xml", this.getClass());
		EventDrivenConsumer dmAdapter = ac.getBean("dmAdapter", EventDrivenConsumer.class);
		MessageHandler handler = TestUtils.getPropertyValue(dmAdapter, "handler", MessageHandler.class);
		assertEquals(DirectMessageSendingMessageHandler.class, handler.getClass());
		assertEquals(23, TestUtils.getPropertyValue(handler, "order"));
		dmAdapter = ac.getBean("dmAdvised", EventDrivenConsumer.class);
		handler = TestUtils.getPropertyValue(dmAdapter, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
		MessageHandler handler2 = TestUtils.getPropertyValue(ac.getBean("advised"), "handler", MessageHandler.class);
		assertNotSame(handler, handler2);
		handler2.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(2, adviceCalled);
	}

	@Test
	public void testInt2718FailForOutboundAdapterWithRequestHandlerAdviceChainWithinChainConfig() {
		try {
			new ClassPathXmlApplicationContext("OutboundAdapterWithRHACWithinChain-fail-context.xml", this.getClass());
			fail("Expected BeanDefinitionParsingException");
		}
		catch (BeansException e) {
			assertTrue(e instanceof BeanDefinitionParsingException);
			assertTrue(e.getMessage().contains("'request-handler-advice-chain' isn't allowed " +
					"for 'outbound-channel-adapter' within <chain>, when its Handler isn't instanceOf AbstractReplyProducingMessageHandler"));
		}
	}


	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
