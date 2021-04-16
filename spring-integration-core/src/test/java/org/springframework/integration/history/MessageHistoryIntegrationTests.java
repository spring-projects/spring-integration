/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.history;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.util.StopWatch;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 */
public class MessageHistoryIntegrationTests {

	private final Log logger = LogFactory.getLog(getClass());

	@Test
	public void testNoHistoryAwareMessageHandler() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml",
				MessageHistoryIntegrationTests.class);
		Map<String, ConsumerEndpointFactoryBean> cefBeans = ac.getBeansOfType(ConsumerEndpointFactoryBean.class);
		for (ConsumerEndpointFactoryBean cefBean : cefBeans.values()) {
			DirectFieldAccessor bridgeAccessor = new DirectFieldAccessor(cefBean);
			Boolean shouldTrack = (Boolean) bridgeAccessor.getPropertyValue("handler.shouldTrack");
			assertThat(shouldTrack).isFalse();
		}
		ac.close();
	}

	@Test
	public void testMessageHistoryWithHistoryWriter() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriter.xml",
				MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() { // Not a lambda: Mockito can't mock final classes

			@Override
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders()
						.get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();

				Properties event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("sampleGateway#echo(String)");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("gateway");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("bridgeInChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("testBridge");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("bridge");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("headerEnricherChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("testHeaderEnricher");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("header-enricher");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("chainChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("sampleChain");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("chain");

				event = historyIterator.next();
				assertThat(event
						.getProperty(MessageHistory.NAME_PROPERTY))
						.isEqualTo("sampleChain$child.service-activator-within-chain");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("service-activator");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("filterChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("testFilter");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("filter");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("splitterChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("testSplitter");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("splitter");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("aggregatorChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("testAggregator");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("aggregator");

				event = historyIterator.next();
				assertThat(event.getProperty(MessageHistory.NAME_PROPERTY)).isEqualTo("endOfThePipeChannel");
				assertThat(event.getProperty(MessageHistory.TYPE_PROPERTY)).isEqualTo("channel");

				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		Message<?> result = gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		assertThat(result).isNotNull();
		//assertEquals("hello", result);
		ac.close();
	}

	@Test
	public void testMessageHistoryWithoutHistoryWriter() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext("messageHistoryWithoutHistoryWriter.xml",
				MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() { // Not a lambda: Mockito can't mock final classes

			@Override
			public void handleMessage(Message<?> message) {
				assertThat(message.getHeaders().get(MessageHistory.HEADER_NAME, MessageHistory.class)).isNull();
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testMessageHistoryParser() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext(
				"messageHistoryWithHistoryWriterNamespace.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() { // Not a lambda: Mockito can't mock final classes

			@Override
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders()
						.get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
				assertThat(historyIterator.hasNext()).isTrue();
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testMessageHistoryParserWithNamePatterns() {
		ConfigurableApplicationContext ac = new ClassPathXmlApplicationContext(
				"messageHistoryWithHistoryWriterNamespaceAndPatterns.xml", MessageHistoryIntegrationTests.class);
		SampleGateway gateway = ac.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = ac.getBean("endOfThePipeChannel", DirectChannel.class);
		MessageHandler handler = Mockito.spy(new MessageHandler() { // Not a lambda: Mockito can't mock final classes

			@Override
			public void handleMessage(Message<?> message) {
				Iterator<Properties> historyIterator = message.getHeaders()
						.get(MessageHistory.HEADER_NAME, MessageHistory.class).iterator();
				assertThat(historyIterator.hasNext()).isTrue();
				Properties gatewayHistory = historyIterator.next();
				assertThat(gatewayHistory.get("name")).isEqualTo("sampleGateway#echo(String)");
				assertThat(historyIterator.hasNext()).isTrue();
				Properties chainHistory = historyIterator.next();
				assertThat(chainHistory.get("name")).isEqualTo("sampleChain");
				assertThat(historyIterator.hasNext()).isFalse();
				MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
				replyChannel.send(message);
			}
		});
		endOfThePipeChannel.subscribe(handler);
		gateway.echo("hello");
		Mockito.verify(handler, Mockito.times(1)).handleMessage(Mockito.any(Message.class));
		ac.close();
	}

	@Test
	public void testMessageHistoryMoreThanOneNamespaceFail() {
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("messageHistoryWithHistoryWriterNamespace-fail.xml",
								MessageHistoryIntegrationTests.class));
	}

	@Test
	@Disabled
	public void testMessageHistoryWithHistoryPerformance() {
		ConfigurableApplicationContext acWithHistory = new ClassPathXmlApplicationContext("perfWithMessageHistory.xml",
				MessageHistoryIntegrationTests.class);
		ConfigurableApplicationContext acWithoutHistory = new ClassPathXmlApplicationContext(
				"perfWithoutMessageHistory.xml", MessageHistoryIntegrationTests.class);

		SampleGateway gatewayHistory = acWithHistory.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannelHistory = acWithHistory.getBean("endOfThePipeChannel", DirectChannel.class);
		endOfThePipeChannelHistory.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
		});

		SampleGateway gateway = acWithoutHistory.getBean("sampleGateway", SampleGateway.class);
		DirectChannel endOfThePipeChannel = acWithoutHistory.getBean("endOfThePipeChannel", DirectChannel.class);
		endOfThePipeChannel.subscribe(message -> {
			MessageChannel replyChannel = (MessageChannel) message.getHeaders().getReplyChannel();
			replyChannel.send(message);
		});

		StopWatch stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			gatewayHistory.echo("hello");
		}
		stopWatch.stop();
		logger.info("Elapsed time with history 10000 calls: " + stopWatch.getTotalTimeSeconds());
		stopWatch = new StopWatch();
		stopWatch.start();
		for (int i = 0; i < 10000; i++) {
			gateway.echo("hello");
		}
		stopWatch.stop();
		logger.info("Elapsed time without history 10000 calls: " + stopWatch.getTotalTimeSeconds());
		acWithHistory.close();
		acWithoutHistory.close();
	}

	public interface SampleGateway {

		Message<?> echo(String value);

	}

}
