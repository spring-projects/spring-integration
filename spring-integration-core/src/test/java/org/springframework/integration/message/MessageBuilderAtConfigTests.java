/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.message;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.ConsumerEndpointFactoryBean;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.message.MessageBuilderAtConfigTests.MBConfig;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilderFactory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Marius Bogoevici
 */
@ContextConfiguration(classes = MBConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
public class MessageBuilderAtConfigTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private PollableChannel out;

	@Autowired
	private MessageBuilderFactory messageBuilderFactory;

	@Test
	public void mutate() {
		assertThat(messageBuilderFactory instanceof MutableMessageBuilderFactory).isTrue();
		in.send(new GenericMessage<String>("foo"));
		Message<?> m1 = out.receive(0);
		Message<?> m2 = out.receive(0);
		assertThat(m1.getClass().getName()).isEqualTo("org.springframework.integration.support.MutableMessage");
		assertThat(m1 == m2).isTrue();
	}

	@Configuration
	@EnableIntegration
	public static class MBConfig {

		@Bean
		public MessageChannel in() {
			return new DirectChannel();
		}

		@Bean
		public PollableChannel out() {
			return new QueueChannel();
		}

		@Bean
		public MessageBuilderFactory messageBuilderFactory() {
			return new MutableMessageBuilderFactory();
		}

		@Bean
		public MessageChannel pubSub() {
			return new PublishSubscribeChannel();
		}

		@Bean
		public ConsumerEndpointFactoryBean echo1() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler1());
			factory.setInputChannel(in());
			return factory;
		}

		@Bean
		public AbstractReplyProducingMessageHandler handler1() {
			AbstractReplyProducingMessageHandler handler = new RequestHeaderCopyingEchoHandler();
			handler.setOutputChannel(pubSub());
			return handler;
		}

		@Bean
		public ConsumerEndpointFactoryBean echo2() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler2());
			factory.setInputChannel(pubSub());
			return factory;
		}

		@Bean
		public AbstractReplyProducingMessageHandler handler2() {
			AbstractReplyProducingMessageHandler handler = new RequestHeaderCopyingEchoHandler();
			handler.setOutputChannel(out());
			return handler;
		}

		@Bean
		public ConsumerEndpointFactoryBean echo3() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler3());
			factory.setInputChannel(pubSub());
			return factory;
		}

		@Bean
		public AbstractReplyProducingMessageHandler handler3() {
			AbstractReplyProducingMessageHandler handler = new RequestHeaderCopyingEchoHandler();
			handler.setOutputChannel(out());
			return handler;
		}

	}

	private static class RequestHeaderCopyingEchoHandler extends AbstractReplyProducingMessageHandler {

		RequestHeaderCopyingEchoHandler() {
			super();
		}

		@Override
		protected Object handleRequestMessage(Message<?> requestMessage) {
			return requestMessage;
		}

	}

}
