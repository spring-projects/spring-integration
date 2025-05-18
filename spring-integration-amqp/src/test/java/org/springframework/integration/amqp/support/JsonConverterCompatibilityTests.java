/*
 * Copyright 2016-2025 the original author or authors.
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

package org.springframework.integration.amqp.support;

import com.rabbitmq.client.AMQP.BasicProperties;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.RabbitAvailable;
import org.springframework.amqp.rabbit.junit.RabbitAvailableCondition;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
@RabbitAvailable(queues = {JsonConverterCompatibilityTests.JSON_TESTQ})
public class JsonConverterCompatibilityTests {

	public static final String JSON_TESTQ = "si.json.tests";

	private RabbitTemplate rabbitTemplate;

	@BeforeEach
	public void setUp() {
		this.rabbitTemplate = new RabbitTemplate(new CachingConnectionFactory("localhost"));
		this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
	}

	@AfterEach
	public void tearDown() {
		RabbitAvailableCondition.getBrokerRunning().removeTestQueues();
		((CachingConnectionFactory) this.rabbitTemplate.getConnectionFactory()).destroy();
	}

	@Test
	public void testInbound() {
		@SuppressWarnings("unchecked") final Message<String> out = (Message<String>) new ObjectToJsonTransformer()
				.transform(new GenericMessage<Foo>(new Foo()));
		MessageProperties messageProperties = new MessageProperties();
		DefaultAmqpHeaderMapper.outboundMapper().fromHeadersToRequest(out.getHeaders(), messageProperties);
		final BasicProperties props = new DefaultMessagePropertiesConverter().fromMessageProperties(messageProperties,
				"UTF-8");
		this.rabbitTemplate.execute(channel -> {
			channel.basicPublish("", JSON_TESTQ, props, out.getPayload().getBytes());
			return null;
		});

		Object received = this.rabbitTemplate.receiveAndConvert(JSON_TESTQ);
		assertThat(received).isInstanceOf(Foo.class);
	}

	public static class Foo {

		private String bar = "bar";

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

	}

}
