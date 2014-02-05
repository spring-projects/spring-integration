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

package org.springframework.integration.history;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
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

				Properties event = historyIterator.next();
				assertEquals("sampleGateway", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("gateway", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("bridgeInChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("testBridge", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("bridge", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("headerEnricherChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("testHeaderEnricher", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("transformer", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("chainChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("sampleChain", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("chain", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("sampleChain$child.service-activator-within-chain", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("service-activator", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("filterChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("testFilter", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("filter", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("splitterChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("testSplitter", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("splitter", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("aggregatorChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("testAggregator", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("aggregator", event.getProperty(MessageHistory.TYPE_PROPERTY));

				event = historyIterator.next();
				assertEquals("endOfThePipeChannel", event.getProperty(MessageHistory.NAME_PROPERTY));
				assertEquals("channel", event.getProperty(MessageHistory.TYPE_PROPERTY));

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

	@Test(expected=BeanCreationException.class)
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
