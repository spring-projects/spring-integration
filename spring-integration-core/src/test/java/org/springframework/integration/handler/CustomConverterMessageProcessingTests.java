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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.convert.converter.Converter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class CustomConverterMessageProcessingTests {

	@Autowired
	private MessageChannel serviceActivatorChannel;

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
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals(TestBean2.class, result.getPayload().getClass());
		assertEquals("SERVICE-TEST", ((TestBean2) result.getPayload()).text);
	}

	@Test
	public void transformer() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("transformer-test"))
				.setReplyChannel(replyChannel).build();
		this.transformerChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals(TestBean2.class, result.getPayload().getClass());
		assertEquals("TRANSFORMER-TEST", ((TestBean2) result.getPayload()).text);
	}

	@Test
	public void splitter() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("splitter-test"))
				.setReplyChannel(replyChannel).build();
		this.splitterChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals(TestBean2.class, result.getPayload().getClass());
		assertEquals("SPLITTER-TEST", ((TestBean2) result.getPayload()).text);
	}

	@Test
	public void filter() {
		QueueChannel replyChannel = new QueueChannel();
		Message<?> message = MessageBuilder.withPayload(new TestBean1("filter-test"))
				.setReplyChannel(replyChannel).build();
		this.filterChannel.send(message);
		Message<?> result = replyChannel.receive(0);
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals(TestBean1.class, result.getPayload().getClass());
		assertEquals("filter-test", ((TestBean1) result.getPayload()).text);
	}

	@Test
	public void router() {
		Message<?> message = MessageBuilder.withPayload(new TestBean1("router-test")).build();
		this.routerChannel.send(message);
		Message<?> result = this.routerTargetChannel.receive(0);
		assertNotNull(result);
		assertNotNull(result.getPayload());
		assertEquals(TestBean1.class, result.getPayload().getClass());
		assertEquals("router-test", ((TestBean1) result.getPayload()).text);
	}


	@SuppressWarnings("unused")
	private static class TestConverter implements Converter<TestBean1, TestBean2> {

		public TestBean2 convert(TestBean1 source) {
			return new TestBean2(source.text.toUpperCase());
		}
	}


	@SuppressWarnings("unused")
	private static class TestService {

		public Object test(TestBean2 bean) {
			return bean;
		}

		public boolean filter(TestBean2 bean) {
			return true;
		}
	}


	private static class TestBean1  {

		private String text;

		public TestBean1(String text) {
			this.text = text;
		}
	}


	private static class TestBean2 {

		private String text;

		public TestBean2(String text) {
			this.text = text;
		}

		// called by router for channel name
		public String toString() {
			return this.text.replace("-TEST", "_TARGET_CHANNEL");
		}
	}

}
