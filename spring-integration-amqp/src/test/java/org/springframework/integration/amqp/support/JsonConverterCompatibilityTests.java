/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.amqp.support;

import java.io.IOException;

import com.rabbitmq.client.AMQP.BasicProperties;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.support.DefaultMessagePropertiesConverter;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class JsonConverterCompatibilityTests implements RabbitTestContainer {

	static final String JSON_TESTQ = "si.json.tests";

	@BeforeAll
	static void initQueue() throws IOException, InterruptedException {
		RABBITMQ.execInContainer("rabbitmqadmin", "declare", "queue", "name=" + JSON_TESTQ);
	}

	@AfterAll
	static void deleteQueue() throws IOException, InterruptedException {
		RABBITMQ.execInContainer("rabbitmqadmin", "delete", "queue", "name=" + JSON_TESTQ);
	}

	private RabbitTemplate rabbitTemplate;

	@BeforeEach
	public void setUp() {
		this.rabbitTemplate = new RabbitTemplate(new CachingConnectionFactory(RabbitTestContainer.amqpPort()));
		this.rabbitTemplate.setMessageConverter(new JacksonJsonMessageConverter("*"));
	}

	@AfterEach
	public void tearDown() {
		((CachingConnectionFactory) this.rabbitTemplate.getConnectionFactory()).destroy();
	}

	@Test
	public void testInbound() {
		@SuppressWarnings("unchecked") final Message<String> out = (Message<String>) new ObjectToJsonTransformer()
				.transform(new GenericMessage<>(new Foo()));
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
