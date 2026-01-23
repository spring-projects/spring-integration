/*
 * Copyright 2016-present the original author or authors.
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

package org.springframework.integration.groovy;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.config.EnableIntegration;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.scripting.dsl.Scripts;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 5.0
 */
@SpringJUnitConfig
@DirtiesContext
public class ScriptsFactoryTests {

	@Autowired
	@Qualifier("scriptFilter.input")
	private MessageChannel filterInput;

	@Autowired
	private PollableChannel discardChannel;

	@Autowired
	private PollableChannel results;

	@Autowired
	private MessageProcessor<?> scriptMessageProcessor;

	@Test
	public void filterTest() {
		Message<?> message1 = MessageBuilder.withPayload("bad").setHeader("type", "bad").build();
		Message<?> message2 = MessageBuilder.withPayload("good").setHeader("type", "good").build();
		this.filterInput.send(message1);
		this.filterInput.send(message2);
		assertThat(this.results.receive(10000).getPayload()).isEqualTo("good");
		assertThat(this.results.receive(0)).isNull();
		assertThat(this.discardChannel.receive(10000).getPayload()).isEqualTo("bad");
		assertThat(this.discardChannel.receive(0)).isNull();

		MessageProcessor<?> delegate = TestUtils.getPropertyValue(this.scriptMessageProcessor, "delegate");

		assertThat(delegate).isInstanceOf(GroovyScriptExecutingMessageProcessor.class);
	}

	@Configuration
	@EnableIntegration
	public static class ContextConfiguration {

		@Bean
		public PollableChannel discardChannel() {
			return new QueueChannel();
		}

		@Bean
		public Resource scriptResource() {
			return new ByteArrayResource("headers.type == 'good'".getBytes());
		}

		@Bean
		public IntegrationFlow scriptFilter() {
			return f -> f
					.filter(Scripts.processor(scriptResource())
									.lang("groovy"),
							e -> e.discardChannel("discardChannel"))
					.channel(c -> c.queue("results"));
		}

	}

}
