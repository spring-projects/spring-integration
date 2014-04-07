/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.message.MessageBuilderAtConfigTests.MBConfig;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.MutableMessageBuilderFacfory;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 */
@ContextConfiguration(classes=MBConfig.class)
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
		assertTrue(messageBuilderFactory instanceof MutableMessageBuilderFacfory);
		in.send(new GenericMessage<String>("foo"));
		Message<?> m1 = out.receive(0);
		Message<?> m2 = out.receive(0);
		assertEquals("org.springframework.integration.support.MutableMessage", m1.getClass().getName());
		assertTrue(m1 == m2);
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
			return new MutableMessageBuilderFacfory();
		}

		@Bean
		public MessageChannel pubSub() {
			return new PublishSubscribeChannel();
		}

		@Bean
		public ConsumerEndpointFactoryBean bridge1() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler1());
			factory.setInputChannel(in());
			return factory;
		}

		@Bean
		public BridgeHandler handler1() {
			BridgeHandler handler = new BridgeHandler();
			handler.setOutputChannel(pubSub());
			return handler;
		}

		@Bean
		public ConsumerEndpointFactoryBean bridge2() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler2());
			factory.setInputChannel(pubSub());
			return factory;
		}

		@Bean
		public BridgeHandler handler2() {
			BridgeHandler handler = new BridgeHandler();
			handler.setOutputChannel(out());
			return handler;
		}

		@Bean
		public ConsumerEndpointFactoryBean bridge3() throws Exception {
			ConsumerEndpointFactoryBean factory = new ConsumerEndpointFactoryBean();
			factory.setHandler(handler3());
			factory.setInputChannel(pubSub());
			return factory;
		}

		@Bean
		public BridgeHandler handler3() {
			BridgeHandler handler = new BridgeHandler();
			handler.setOutputChannel(out());
			return handler;
		}

	}

}
