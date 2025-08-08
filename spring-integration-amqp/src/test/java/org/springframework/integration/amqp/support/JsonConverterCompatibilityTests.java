/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import com.rabbitmq.client.AMQP.BasicProperties;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
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
public class JsonConverterCompatibilityTests {

	private static final String JSON_TESTQ = "si.json.tests";

	@Rule
	public BrokerRunning brokerRunning = BrokerRunning.isRunningWithEmptyQueues(JSON_TESTQ);

	private RabbitTemplate rabbitTemplate;

	@Before
	public void setUp() {
		this.rabbitTemplate = new RabbitTemplate(new CachingConnectionFactory("localhost"));
		this.rabbitTemplate.setMessageConverter(new Jackson2JsonMessageConverter());
	}

	@After
	public void tearDown() {
		this.brokerRunning.removeTestQueues();
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
