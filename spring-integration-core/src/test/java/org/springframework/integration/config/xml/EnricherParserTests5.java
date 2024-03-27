/*
 * Copyright 2014-2024 the original author or authors.
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
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests the error-channel in an enricher to produce
 * a default object in case of downstream failure.
 *
 * @author Kris Jacyna
 * @since 4.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class EnricherParserTests5 {

	@Autowired
	private ApplicationContext context;

	@Test
	public void errorChannelTest() {

		class ErrorThrower extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				throw new RuntimeException();
			}

		}

		class DefaultTargetProducer extends AbstractReplyProducingMessageHandler {

			@Override
			protected Object handleRequestMessage(Message<?> requestMessage) {
				final Target defaultTarget = new Target();
				defaultTarget.setName("Default");
				return defaultTarget;
			}

		}

		context.getBean("requestChannel", DirectChannel.class).subscribe(new ErrorThrower());
		context.getBean("errChannel", DirectChannel.class).subscribe(new DefaultTargetProducer());

		Target original = new Target();
		original.setName("John");
		Message<?> request = MessageBuilder.withPayload(original).build();

		context.getBean("inputChannel", DirectChannel.class).send(request);

		Message<?> reply = context.getBean("outputChannel", PollableChannel.class).receive(10000);
		Target enriched = (Target) reply.getPayload();
		assertThat(enriched.getName()).isEqualTo("Mr. Default");
	}

	public static class Target {

		private volatile String name;

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

	}

}
