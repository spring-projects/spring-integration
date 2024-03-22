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
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

import io.nats.client.Connection;
import io.nats.client.JetStreamApiException;
import io.nats.client.Message;
import io.nats.client.api.PublishAck;
import io.nats.client.impl.Headers;
import io.nats.client.impl.NatsMessage;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;

/**
 * NatsTemplate to send nats messages to specified subject
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
public class NatsTemplate {

	private static final Log LOG = LogFactory.getLog(NatsTemplate.class);

	@NonNull
	private final Connection natsConnection;

	@NonNull
	private final String subject;

	@NonNull
	private final MessageConverter<?> messageConverter;

	public NatsTemplate(
			@NonNull final Connection pNatsConnection,
			@NonNull final String pSubject,
			@NonNull final MessageConverter<?> pMessageConverter) {
		this.natsConnection = pNatsConnection;
		this.subject = pSubject;
		this.messageConverter = pMessageConverter;
	}

	@Nullable
	public PublishAck send(@NonNull final Object message) throws IOException, JetStreamApiException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Publishing NATS message to subject: " + getSubject() + " message: " + message);
		}
		return this.natsConnection
				.jetStream()
				.publish(this.subject, this.messageConverter.toMessage(message));
	}

	@Nullable
	public CompletableFuture<PublishAck> sendAsync(@NonNull final Object message)
			throws IOException, JetStreamApiException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Publishing NATS message to subject: " + getSubject() + " message: " + message);
		}
		return this.natsConnection
				.jetStream()
				.publishAsync(this.subject, this.messageConverter.toMessage(message));
	}

	@Nullable
	public PublishAck send(@NonNull final Object message, @NonNull final Headers headers)
			throws IOException, JetStreamApiException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Publishing NATS message to subject: " + getSubject() + " message: " + message);
		}
		final Message natsMessage =
				NatsMessage.builder()
						.data(this.messageConverter.toMessage(message))
						.headers(headers)
						.subject(this.subject)
						.build();
		return this.natsConnection.jetStream().publish(natsMessage);
	}

	@Nullable
	public CompletableFuture<PublishAck> sendAsync(
			@NonNull final Object message, @NonNull final Headers headers)
			throws IOException, JetStreamApiException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Publishing NATS message to subject: " + getSubject() + " message: " + message);
		}
		final Message natsMessage =
				NatsMessage.builder()
						.data(this.messageConverter.toMessage(message))
						.headers(headers)
						.subject(this.subject)
						.build();
		return this.natsConnection.jetStream().publishAsync(natsMessage);
	}

	@Nullable
	public Message requestReply(
			@NonNull final Object message,
			@NonNull final Headers headers,
			@NonNull final Duration timeout)
			throws IOException, JetStreamApiException, InterruptedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug(
					"Publishing NATS message to subject: "
							+ getSubject()
							+ " message: "
							+ message
							+ " headers: "
							+ headers.getFirst("nats_replyTo"));
		}
		final Message natsMessage =
				NatsMessage.builder()
						.data(this.messageConverter.toMessage(message))
						.headers(headers)
						.subject(this.subject)
						.build();
		return this.natsConnection.request(natsMessage, timeout);
	}

	@Nullable
	public void publishReply(
			@NonNull final Object message, @NonNull final Headers headers, @NonNull final String replyTo)
			throws IOException, JetStreamApiException, InterruptedException {
		if (LOG.isDebugEnabled()) {
			LOG.debug("Publishing NATS message to reply subject: " + replyTo + " message: " + message);
		}
		this.natsConnection.publish(replyTo, this.messageConverter.toMessage(message));
	}

	@NonNull
	public String getSubject() {
		return this.subject;
	}
}
