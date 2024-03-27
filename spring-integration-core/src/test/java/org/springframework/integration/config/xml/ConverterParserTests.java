/*
 * Copyright 2002-2024 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConverterParserTests {

	@Autowired
	@Qualifier("serviceActivatorChannel")
	private MessageChannel serviceActivatorChannel;

	@Autowired
	@Qualifier("serviceActivatorChannel3")
	private MessageChannel serviceActivatorChannel3;

	@Autowired
	private MessageChannel transformerChannel;

	@Autowired
	private MessageChannel splitterChannel;

	@Autowired
	private MessageChannel filterChannel;

	@Autowired
	private MessageChannel routerChannel;

	@Autowired
	@Qualifier("ROUTER_TARGET_CHANNEL")
	private PollableChannel routerTargetChannel;

	@Test
	public void serviceActivator() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("service-test"))
				.setReplyChannel(replyChannel).build();
		this.serviceActivatorChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean2.class);
		assertThat(((TestBean2) result.getPayload()).text).isEqualTo("SERVICE-TEST");
	}

	@Test
	public void serviceActivatorUsingInnerConverterDefinition() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("service-test"))
				.setReplyChannel(replyChannel).build();
		this.serviceActivatorChannel3.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean3.class);
		assertThat(((TestBean3) result.getPayload()).text).isEqualTo("SERVICE-TEST");
	}

	@Test
	public void transformer() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("transformer-test"))
				.setReplyChannel(replyChannel).build();
		this.transformerChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean2.class);
		assertThat(((TestBean2) result.getPayload()).text).isEqualTo("TRANSFORMER-TEST");
	}

	@Test
	public void splitter() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("splitter-test"))
				.setReplyChannel(replyChannel).build();
		this.splitterChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean2.class);
		assertThat(((TestBean2) result.getPayload()).text).isEqualTo("SPLITTER-TEST");
	}

	@Test
	public void filter() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("filter-test"))
				.setReplyChannel(replyChannel).build();
		this.filterChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean1.class);
		assertThat(((TestBean1) result.getPayload()).text).isEqualTo("filter-test");
	}

	@Test
	public void router() {
		Message<?> message = MessageBuilder.withPayload(new TestBean1("router-test")).build();
		this.routerChannel.send(message);
		Message<?> result = this.routerTargetChannel.receive(0);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isNotNull();
		assertThat(result.getPayload().getClass()).isEqualTo(TestBean1.class);
		assertThat(((TestBean1) result.getPayload()).text).isEqualTo("router-test");
	}

	@SuppressWarnings("unused")
	public static class TestService {

		public Object test(TestBean2 bean) {
			return bean;
		}

		public Object test3(TestBean3 bean) {
			return bean;
		}

		public boolean filter(TestBean2 bean) {
			return true;
		}

	}

	public static class TestBean1 {

		private String text;

		public TestBean1(String text) {
			this.text = text;
		}

	}

	public static class TestBean2 {

		private String text;

		public TestBean2(String text) {
			this.text = text;
		}

		// called by router for channel name
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}

	}

	private static class TestBean3 {

		private String text;

		TestBean3(String text) {
			this.text = text;
		}

		// called by router for channel name
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}

	}

	@SuppressWarnings("unused")
	private static class TestConverter implements Converter<TestBean1, TestBean2> {

		public TestBean2 convert(TestBean1 source) {
			return new TestBean2(source.text.toUpperCase());
		}

	}

	@SuppressWarnings("unused")
	private static class TestConverter3 implements Converter<TestBean1, TestBean3> {

		public TestBean3 convert(TestBean1 source) {
			return new TestBean3(source.text.toUpperCase());
		}

	}

}
