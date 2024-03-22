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
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.nats.util.NatsUtils;
import org.springframework.lang.NonNull;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;

/**
 * NatsMessageProducingHandler to send nats messages through MessageHandler
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
public class NatsMessageProducingHandler extends AbstractMessageHandler {

	private static final Log LOG = LogFactory.getLog(NatsMessageProducingHandler.class);

	@NonNull
	private final NatsTemplate natsTemplate;

	public NatsMessageProducingHandler(@NonNull final NatsTemplate pNatsTemplate) {
		this.natsTemplate = pNatsTemplate;
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) {
		final Object payload = message.getPayload();
		if (payload != null) {
			try {
				LOG.debug("Publishing message to subject: " + this.natsTemplate.getSubject());
				final PublishAck publishAck =
						this.natsTemplate.send(payload, NatsUtils.populateNatsMessageHeaders(message));
				if (publishAck != null) {
					LOG.debug("Nats Message sent " + message.getPayload() + " " + publishAck);
				}
			}
			catch (IOException | JetStreamApiException e) {
				throw new MessageDeliveryException(
						message,
						"Exception occurred while sending message to " + this.natsTemplate.getSubject(),
						e);
			}
		}
	}
}
