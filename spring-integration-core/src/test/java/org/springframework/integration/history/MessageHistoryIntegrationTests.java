/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.core.MessageHandler;
import org.springframework.util.StopWatch;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class MessageHistoryIntegrationTests {

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
				Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();

				Properties event1 = historyIterator.next();
				assertEquals("sampleGateway", event1.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("gateway", event1.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event2 = historyIterator.next();
				assertEquals("bridgeInChannel", event2.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event2.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event3 = historyIterator.next();
				assertEquals("testBridge", event3.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("bridge", event3.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event4 = historyIterator.next();
				assertEquals("headerEnricherChannel", event4.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event4.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event5 = historyIterator.next();
				assertEquals("testHeaderEnricher", event5.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("transformer", event5.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event6 = historyIterator.next();
				assertEquals("chainChannel", event6.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event6.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event7 = historyIterator.next();
				assertEquals("sampleChain", event7.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("chain", event7.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event8 = historyIterator.next();
				assertEquals("filterChannel", event8.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event8.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event9 = historyIterator.next();
				assertEquals("testFilter", event9.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("filter", event9.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event10 = historyIterator.next();
				assertEquals("splitterChannel", event10.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event10.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event11 = historyIterator.next();
				assertEquals("testSplitter", event11.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("splitter", event11.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event12 = historyIterator.next();
				assertEquals("aggregatorChannel", event12.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event12.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event13 = historyIterator.next();
				assertEquals("testAggregator", event13.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("aggregator", event13.getProperty(MessageHistory.TYPE_PROPERTY));

				Properties event14 = historyIterator.next();
				assertEquals("endOfThePipeChannel", event14.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event14.getProperty(MessageHistory.TYPE_PROPERTY));

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
				assertNull(message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class));
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
				Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
				assertTrue(historyIterator.hasNext());
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test
	public void testMessageHistoryParserWithNamePatterns() {
		ApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriterNamespaceAndPatterns.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() {
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
				assertTrue(historyIterator.hasNext());
				Properties gatewayHistory = historyIterator.next();
				assertEquals("sampleGateway", gatewayHistory.get("name"));
				assertTrue(historyIterator.hasNext());
				Properties chainHistory = historyIterator.next();
				assertEquals("sampleChain", chainHistory.get("name"));
				assertFalse(historyIterator.hasNext());
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
	}

	@Test(expected=BeanDefinitionParsingException.class)
	public void testMessageHistoryMoreThanOneNamespaceFail() {
		new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriterNamespace-fail.xml", MessageHistoryIntegrationTests.class);
	}

	@Test @Ignore
	public void testMessageHistoryWithHistoryPerformance() {
		ApplicationContext acWithHistory = new ClassPathXmlApplicationContext("perfWithMessageHistory.xml", MessageHistoryIntegrationTests.class);
		ApplicationContext acWithoutHistory = new ClassPathXmlApplicationContext("perfWithoutMessageHistory.xml", MessageHistoryIntegrationTests.class);

		SampleGateway gatewayHistory = acWithHistory.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannelHistory = acWithHistory.getBean("endOfThePipeChannel", DirectChannel.class);
		endOfThePipeChannelHistory.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message)
					throws MessageRejectedException, MessageHandlingException,
					MessageDeliveryException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});

		SampleGateway gateway = acWithoutHistory.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = acWithoutHistory.getBean("endOfThePipeChannel", DirectChannel.class);
		endOfThePipeChannel.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message)
					throws MessageRejectedException, MessageHandlingException,
					MessageDeliveryException {
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			gatewayHistory.echo("hello");
		}
		stopWatch.stop();
		System.out.println("Elapsed time with history 10000 calls: " + stopWatch.getTotalTimeSeconds());
		stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			gateway.echo("hello");
		}
		stopWatch.stop();
		System.out.println("Elapsed time without history 10000 calls: " + stopWatch.getTotalTimeSeconds());
	}

	public static interface SampleGateway {
		public Message<?> echo(String value);
	}

}
