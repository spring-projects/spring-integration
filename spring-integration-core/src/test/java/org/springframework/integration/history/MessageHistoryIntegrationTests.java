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
import static org.junit.Assert.assertNull;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHandler;

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
			assertEquals("org.springframework.integration.config.MessageHistoryWritingMessageHandler", handlerClassName);
		}
	}

	@Test
	public void testNoHistoryAwareMessageHandler() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		Map<String, ConsumerEndpointFactoryBean> cefBeans = ac.getBeansOfType(ConsumerEndpointFactoryBean.class);
		for (ConsumerEndpointFactoryBean cefBean : cefBeans.values()) {
			DirectFieldAccessor bridgeAccessor = new DirectFieldAccessor(cefBean);
			String handlerClassName = bridgeAccessor.getPropertyValue("handler").getClass().getName();
			assertFalse("org.springframework.integration.config.MessageHistoryWritingMessageHandler".equals(handlerClassName));
		}
	}

	@Test
	public void testMessageHistoryWithHistoryWriter() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders().getHistory().iterator();
				
				Properties event1 = historyIterator.next();
				assertEquals("sampleGateway", event1.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("gateway", event1.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event2 = historyIterator.next();
				assertEquals("bridgeInChannel", event2.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event2.getProperty(MessageHistoryWriter.TYPE_PROPERTY));
				
				Properties event3 = historyIterator.next();
				assertEquals("testBridge", event3.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("bridge", event3.getProperty(MessageHistoryWriter.TYPE_PROPERTY));
				
				Properties event4 = historyIterator.next();
				assertEquals("headerEnricherChannel", event4.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event4.getProperty(MessageHistoryWriter.TYPE_PROPERTY));
				
				Properties event5 = historyIterator.next();
				assertEquals("testHeaderEnricher", event5.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("transformer", event5.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event6 = historyIterator.next();
				assertEquals("chainChannel", event6.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event6.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event7 = historyIterator.next();
				assertEquals("sampleChain", event7.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("chain", event7.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event8 = historyIterator.next();
				assertEquals("filterChannel", event8.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event8.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event9 = historyIterator.next();
				assertEquals("testFilter", event9.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("filter", event9.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event10 = historyIterator.next();
				assertEquals("splitterChannel", event10.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event10.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event11 = historyIterator.next();
				assertEquals("testSplitter", event11.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("splitter", event11.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event12 = historyIterator.next();
				assertEquals("aggregatorChannel", event12.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event12.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event13 = historyIterator.next();
				assertEquals("testAggregator", event13.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("aggregator", event13.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

				Properties event14 = historyIterator.next();
				assertEquals("endOfThePipeChannel", event14.getProperty(MessageHistoryWriter.NAME_PROPERTY));
				assertEquals("channel", event14.getProperty(MessageHistoryWriter.TYPE_PROPERTY));

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
	public void testMessageHistoryWithoutHistoryWriter() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {	
			public void handleMessage(Message<?> message) {
				assertNull(message.getHeaders().getHistory());
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
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders().getHistory().iterator();
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
