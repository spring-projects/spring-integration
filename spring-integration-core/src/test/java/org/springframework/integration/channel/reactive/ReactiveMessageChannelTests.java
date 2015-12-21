/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.channel.reactive;

import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.ReactiveMessageChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import reactor.Processors;

/**
 * @author Artem Bilan
 * @since 5.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class ReactiveMessageChannelTests {

	@Autowired
	private MessageChannel reactiveChannel;

	@Test
	@SuppressWarnings("unchecked")
	public void testReactiveMessageChannel() throws InterruptedException {
		QueueChannel replyChannel = new QueueChannel();

		for (int i = 0; i < 10; i++) {
			this.reactiveChannel.send(MessageBuilder.withPayload(i).setReplyChannel(replyChannel).build());
		}

		for (int i = 0; i < 10; i++) {
			Message<?> receive = replyChannel.receive(10000);
			assertNotNull(receive);
			System.out.println("Receive: " + receive.getPayload());
		}
	}

	@Configuration
	@EnableIntegration
	public static class TestConfiguration {

		@Bean
		public MessageChannel reactiveChannel() {
			return new ReactiveMessageChannel(Processors.queue());
		}

		@ServiceActivator(inputChannel = "reactiveChannel")
		public String handle(int payload) {
			System.out.println("CurrentThread: " + Thread.currentThread() + " for payload: " + payload);
			return "" + payload;
		}

	}

}
