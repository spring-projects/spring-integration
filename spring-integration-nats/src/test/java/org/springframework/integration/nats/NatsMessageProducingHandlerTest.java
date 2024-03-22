/*
 * Copyright 2016-2024 the original author or authors.
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

package org.springframework.integration.nats;

import java.io.IOException;

import io.nats.client.JetStreamApiException;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import org.junit.Test;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Unit test cases for the NatsMessageProducingHandler class.
*
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public class NatsMessageProducingHandlerTest {

	/**
	 * Unit test {@link NatsMessageProducingHandler#handleMessageInternal(Message)} with valid
	 * payload.
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testHandleMessageInternal() throws IOException, JetStreamApiException {
		final NatsTemplate natsTemplate = mock(NatsTemplate.class);
		final NatsMessageProducingHandler producer = new NatsMessageProducingHandler(natsTemplate);
		final Message message = MessageBuilder.withPayload("Test Payload").build();
		when(natsTemplate.getSubject()).thenReturn("Junit");
		when(natsTemplate.send(message.getPayload(), NatsUtils.populateNatsMessageHeaders(message)))
				.thenReturn(mock(PublishAck.class));
		producer.handleMessageInternal(message);
		verify(natsTemplate, times(1)).getSubject();
		verify(natsTemplate, times(1)).send(any(Object.class), any(Headers.class));
	}

	/**
	 * Unit test {@link NatsMessageProducingHandler#handleMessageInternal(Message)} with null payload.
	 *
	 * @throws IOException covers various communication issues with the NATS server such as timeout or
	 *     interruption
	 * @throws JetStreamApiException the request had an error related to the data
	 */
	@Test
	public void testHandleMessageInternalWithNullPayload() throws IOException, JetStreamApiException {
		final NatsTemplate natsTemplate = mock(NatsTemplate.class);
		final NatsMessageProducingHandler producer = new NatsMessageProducingHandler(natsTemplate);
		final Message message = mock(Message.class);
		when(message.getPayload()).thenReturn(null);
		producer.handleMessageInternal(message);
		verify(natsTemplate, times(0)).getSubject();
		verify(natsTemplate, times(0)).send(any(Object.class), any(Headers.class));
	}
}
