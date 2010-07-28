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
package org.springframework.integration.history;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;

import java.util.Iterator;
import java.util.Map;

import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;

/**
 * @author Oleg Zhurakousky
 */
public class MessageHistoryIntegrationTests {

	@Test
	public void testHistoryAwareMessageHandler() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		Map<String, ConsumerEndpointFactoryBean> cefBeans = ac.getBeansOfType(ConsumerEndpointFactoryBean.class);
		for (ConsumerEndpointFactoryBean cefBean : cefBeans.values()) {
			DirectFieldAccessor bridgeAccessor = new DirectFieldAccessor(cefBean);
			String handlerClassName = bridgeAccessor.getPropertyValue("handler").getClass().getName();
			assertEquals("org.springframework.integration.config.MessageHistoryAwareMessageHandler", handlerClassName);
		}
	}

	@Test
	public void testNoHistoryAwareMessageHandler() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		Map<String, ConsumerEndpointFactoryBean> cefBeans = ac.getBeansOfType(ConsumerEndpointFactoryBean.class);
		for (ConsumerEndpointFactoryBean cefBean : cefBeans.values()) {
			DirectFieldAccessor bridgeAccessor = new DirectFieldAccessor(cefBean);
			String handlerClassName = bridgeAccessor.getPropertyValue("handler").getClass().getName();
			assertFalse("org.springframework.integration.config.MessageHistoryAwareMessageHandler".equals(handlerClassName));
		}
	}

	@Test
	public void tetsMessageHistoryWithHistoryWriter() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {
			public void handleMessage(Message<?> message)
								throws MessageRejectedException, MessageHandlingException,MessageDeliveryException {
				System.out.println(message);
				Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
				//1
				MessageHistoryEvent event = historyIterator.next();
				assertEquals("gateway", event.getType());
				assertEquals("sampleGateway", event.getName());
				//2
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("bridgeInChannel", event.getName());
				//3
				event = historyIterator.next();
				assertEquals("bridge", event.getType());
				assertEquals("testBridge", event.getName());
				//4
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("headerEnricherChannel", event.getName());
				//5
				event = historyIterator.next();
				assertEquals("transformer", event.getType());
				assertEquals("testHeaderEnricher", event.getName());
				//6
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("chainChannel", event.getName());
				//7
				event = historyIterator.next();
				assertEquals("chain", event.getType());
				assertEquals("sampleChain", event.getName());
				//8
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("filterChannel", event.getName());
				//9
				event = historyIterator.next();
				assertEquals("filter", event.getType());
				assertEquals("testFilter", event.getName());
				//10
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("splitterChannel", event.getName());
				//11
				event = historyIterator.next();
				assertEquals("splitter", event.getType());
				assertEquals("testSplitter", event.getName());
				//12
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("aggregatorChannel", event.getName());
				//13
				event = historyIterator.next();
				assertEquals("aggregator", event.getType());
				assertEquals("testAggregator", event.getName());
				//
				event = historyIterator.next();
				assertEquals("channel", event.getType());
				assertEquals("endOfThePipeChannel", event.getName());
				
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		Message<?> result = gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		assertNotNull(result);
		//assertEquals("hello", result);
	}
	
	@Test
	public void tetsMessageHistoryWithoutHistoryWriter() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {	
			public void handleMessage(Message<?> message)
								throws MessageRejectedException, MessageHandlingException,MessageDeliveryException {
				System.out.println(message);
				Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
				assertFalse(historyIterator.hasNext());
				
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testMessageHistoryParser() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriterNamespace.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {	
			public void handleMessage(Message<?> message)
								throws MessageRejectedException, MessageHandlingException,MessageDeliveryException {
				System.out.println(message);
				Iterator<MessageHistoryEvent> historyIterator = message.getHeaders().getHistory().iterator();
				assertTrue(historyIterator.hasNext());
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testMessageHistoryMoreThenOneNamespaceFail() {
		new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriterNamespace-fail.xml", MessageHistoryIntegrationTests.class);
	}

	@Test(expected=BeanCreationException.class)
	public void testMessageHistoryMoreThenOneFail() {
		new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriter-fail.xml", MessageHistoryIntegrationTests.class);
	}


	public static interface SampleGateway {
		public Message<?> echo(String value);
	}
}
