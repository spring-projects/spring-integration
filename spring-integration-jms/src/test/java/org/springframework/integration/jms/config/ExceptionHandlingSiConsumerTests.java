/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import javax.jms.Destination;
import javax.jms.Message;
import javax.jms.TextMessage;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

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
