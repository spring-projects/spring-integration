/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.xml.XmlBeanDefinitionReader;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.InputStreamResource;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class InnerDefinitionHandlerAwareEndpointParserTests {

	@Autowired
	private Properties testConfigurations;

	private ConfigurableApplicationContext applicationContext;

	@AfterEach
	public void tearDown() {
		if (this.applicationContext != null) {
			this.applicationContext.close();
		}
	}

	@Test
	public void testInnerSplitterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("splitter-inner-success");
		testSplitterDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerSplitterDefinitionSuccessWithPoller() {
		String configProperty = testConfigurations.getProperty("splitter-inner-success-with-poller");
		this.bootStrap(configProperty);
	}

	@Test
	public void testInnerSplitterDefinitionSuccessWithPollerReversedOrder() {
		String configProperty = testConfigurations.getProperty("splitter-inner-success-with-poller-reversed-order");
		this.bootStrap(configProperty);
	}

	@Test
	public void testRefSplitterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("splitter-ref-success");
		this.testSplitterDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerSplitterDefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("splitter-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	@Test
	public void testInnerTransformerDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("transformer-inner-success");
		this.testTransformerDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefTransformerDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("transformer-ref-success");
		testTransformerDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerTransformerDefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("transformer-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	@Test
	public void testInnerRouterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("router-inner-success");
		testRouterDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefRouterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("router-ref-success");
		testRouterDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerRouterDefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("router-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	@Test
	public void testInnerSADefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("sa-inner-success");
		this.testSADefinitionSuccess(configProperty);
	}

	@Test
	public void testRefSADefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("sa-ref-success");
		this.testSADefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerSADefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("sa-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	@Test
	public void testInnerAggregatorDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("aggregator-inner-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerConcurrentAggregatorDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("aggregator-inner-concurrent-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerConcurrentAggregatorDefinitionSuccessReorderBeanPoller() {
		String configProperty = testConfigurations
				.getProperty("aggregator-inner-concurrent-success-reorder-bean-poller");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefAggregatorDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("aggregator-ref-success");
		this.testAggregatorDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerAggregatorDefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("aggregator-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	@Test
	public void testInnerFilterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("filter-inner-success");
		this.testFilterDefinitionSuccess(configProperty);
	}

	@Test
	public void testRefFilterDefinitionSuccess() {
		String configProperty = testConfigurations.getProperty("filter-ref-success");
		this.testFilterDefinitionSuccess(configProperty);
	}

	@Test
	public void testInnerFilterDefinitionFailureRefAndInner() {
		String xmlConfig = testConfigurations.getProperty("filter-failure-refAndBean");
		assertThatExceptionOfType(BeanDefinitionStoreException.class)
				.isThrownBy(() -> bootStrap(xmlConfig));
	}

	private void testSplitterDefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		EventDrivenConsumer splitter = this.applicationContext.getBean("testSplitter", EventDrivenConsumer.class);
		assertThat(splitter).isNotNull();
		MessageBuilder<String[]> inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{ "One", "Two" });
		Message<String[]> inMessage = inChannelMessageBuilder.build();
		MessageChannel inChannel = this.applicationContext.getBean("inChannel", MessageChannel.class);
		inChannel.send(inMessage);
		PollableChannel outChannel = this.applicationContext.getBean("outChannel", PollableChannel.class);
		assertThat(outChannel.receive().getPayload() instanceof String).isTrue();
		outChannel = this.applicationContext.getBean("outChannel", PollableChannel.class);
		assertThat(outChannel.receive().getPayload() instanceof String).isTrue();
	}

	private void testTransformerDefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		EventDrivenConsumer transformer = this.applicationContext.getBean("testTransformer", EventDrivenConsumer.class);
		assertThat(transformer).isNotNull();
		MessageBuilder<String[]> inChannelMessageBuilder = MessageBuilder.withPayload(new String[]{ "One", "Two" });
		Message<String[]> inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = this.applicationContext.getBean("inChannel", DirectChannel.class);
		inChannel.send(inMessage);
		PollableChannel outChannel = this.applicationContext.getBean("outChannel", PollableChannel.class);
		String payload = (String) outChannel.receive().getPayload();
		assertThat(payload.equals("One,Two")).isTrue();
	}

	private void testRouterDefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		EventDrivenConsumer splitter = this.applicationContext.getBean("testRouter", EventDrivenConsumer.class);
		assertThat(splitter).isNotNull();
		MessageBuilder<String> inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message<String> inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = this.applicationContext.getBean("inChannel", DirectChannel.class);
		inChannel.send(inMessage);
		PollableChannel channel1 = this.applicationContext.getBean("channel1", PollableChannel.class);
		assertThat(channel1.receive().getPayload().equals("1")).isTrue();
		inChannelMessageBuilder = MessageBuilder.withPayload("2");
		inMessage = inChannelMessageBuilder.build();
		inChannel.send(inMessage);
		PollableChannel channel2 = this.applicationContext.getBean("channel2", PollableChannel.class);
		assertThat(channel2.receive().getPayload().equals("2")).isTrue();
	}

	private void testSADefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		EventDrivenConsumer splitter = this.applicationContext
				.getBean("testServiceActivator", EventDrivenConsumer.class);
		assertThat(splitter).isNotNull();
		MessageBuilder<String> inChannelMessageBuilder = MessageBuilder.withPayload("1");
		Message<String> inMessage = inChannelMessageBuilder.build();
		DirectChannel inChannel = this.applicationContext.getBean("inChannel", DirectChannel.class);
		inChannel.send(inMessage);
		PollableChannel channel1 = this.applicationContext.getBean("outChannel", PollableChannel.class);
		assertThat(channel1.receive().getPayload().equals("1")).isTrue();
	}

	private void testAggregatorDefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		MessageChannel inChannel = this.applicationContext.getBean("inChannel", MessageChannel.class);
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			Message<Integer> message = MessageBuilder.withPayload(i).copyHeaders(headers).build();
			inChannel.send(message);
		}
		PollableChannel output = this.applicationContext.getBean("outChannel", PollableChannel.class);
		Message<?> receivedMessage = output.receive(10000);
		assertThat(receivedMessage).isNotNull();
		assertThat(receivedMessage.getPayload()).isEqualTo(0 + 1 + 2 + 3 + 4);
	}

	private void testFilterDefinitionSuccess(String configProperty) {
		bootStrap(configProperty);
		MessageChannel input = this.applicationContext.getBean("inChannel", MessageChannel.class);
		PollableChannel output = this.applicationContext.getBean("outChannel", PollableChannel.class);
		input.send(new GenericMessage<>("foo"));
		Message<?> reply = output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("foo");
	}

	private void bootStrap(String configProperty) {
		ByteArrayInputStream stream = new ByteArrayInputStream(configProperty.getBytes());
		this.applicationContext = new GenericApplicationContext();
		XmlBeanDefinitionReader reader = new XmlBeanDefinitionReader((BeanDefinitionRegistry) this.applicationContext);
		reader.setValidationMode(XmlBeanDefinitionReader.VALIDATION_XSD);
		reader.loadBeanDefinitions(new InputStreamResource(stream));
		this.applicationContext.refresh();
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correllationId);
		return headers;
	}

	public static class TestSplitter {

		public Collection<String> split(String[] payload) {
			return Arrays.asList(payload);
		}

	}

	public static class TestTransformer {

		public String split(String[] payload) {
			return StringUtils.arrayToDelimitedString(payload, ",");
		}

	}

	public static class TestRouter {

		public String route(String value) {
			return (value.equals("1")) ? "channel1" : "channel2";
		}

	}

	public static class TestServiceActivator {

		public String foo(String value) {
			return value;
		}

	}

	public static class TestAggregator {

		public Integer sum(List<Integer> numbers) {
			int result = 0;
			for (Integer number : numbers) {
				result += number;
			}
			return result;
		}

	}

	public static class TestMessageFilter {

		public boolean filter(String value) {
			return value.equals("foo");
		}

	}

}
