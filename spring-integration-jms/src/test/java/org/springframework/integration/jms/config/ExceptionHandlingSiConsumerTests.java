/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import jakarta.jms.Destination;
import jakarta.jms.Message;
import jakarta.jms.TextMessage;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ExceptionHandlingSiConsumerTests extends ActiveMQMultiContextTests {

	@Autowired
	ApplicationContext applicationContext;

	@Test
	public void nonSiProducer_siConsumer_sync_withReturn() {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		Destination request = applicationContext.getBean("requestQueueA", Destination.class);
		final Destination reply = applicationContext.getBean("replyQueueA", Destination.class);
		jmsTemplate.send(request, session -> {
			TextMessage message = session.createTextMessage();
			message.setText("echoChannel");
			message.setJMSReplyTo(reply);
			return message;
		});
		Message message = jmsTemplate.receive(reply);
		assertThat(message).isNotNull();
	}

	@Test
	public void nonSiProducer_siConsumer_sync_withReturnNoException() throws Exception {
		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		Destination request = applicationContext.getBean("requestQueueB", Destination.class);
		final Destination reply = applicationContext.getBean("replyQueueB", Destination.class);
		jmsTemplate.send(request, session -> {
			TextMessage message = session.createTextMessage();
			message.setText("echoWithExceptionChannel");
			message.setJMSReplyTo(reply);
			return message;
		});
		Message message = jmsTemplate.receive(reply);
		assertThat(message).isNotNull();
		assertThat(((TextMessage) message).getText()).isEqualTo("echoWithException");
	}

	@Test
	public void nonSiProducer_siConsumer_sync_withOutboundGateway() {
		SampleGateway gateway = applicationContext.getBean("sampleGateway", SampleGateway.class);
		String reply = gateway.echo("echoWithExceptionChannel");
		assertThat(reply).isEqualTo("echoWithException");
	}

	public static class SampleService {

		public String echoWithException(String value) {
			throw new SampleException("echoWithException");
		}

		public String echo(String value) {
			return value;
		}

	}

	@SuppressWarnings("serial")
	public static class SampleException extends RuntimeException {

		public SampleException(String message) {
			super(message);
		}

	}

	public interface SampleGateway {

		String echo(String value);

	}

	public static class SampleErrorTransformer {

		public org.springframework.messaging.Message<?> transform(Throwable t) {
			return MessageBuilder.withPayload(t.getCause().getMessage()).build();
		}

	}

}
