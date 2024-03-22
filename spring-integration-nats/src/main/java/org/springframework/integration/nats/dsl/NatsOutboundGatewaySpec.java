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

package org.springframework.integration.nats.dsl;

import io.nats.client.Connection;

import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.nats.NatsOutboundGateway;
import org.springframework.integration.nats.NatsTemplate;
import org.springframework.integration.nats.converter.MessageConverter;
import org.springframework.lang.NonNull;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link MessageHandlerSpec} implementation for the {@link NatsOutboundGateway}.
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
public class NatsOutboundGatewaySpec
		extends MessageHandlerSpec<NatsOutboundGatewaySpec, NatsOutboundGateway> {

	public static NatsOutboundGatewaySpec of(
			@NonNull final NatsTemplate natsTemplate, @NonNull final MessageConverter messageConverter) {
		NatsOutboundGatewaySpec spec = new NatsOutboundGatewaySpec();
		spec.init(natsTemplate, messageConverter);
		return spec;
	}

	public static NatsOutboundGatewaySpec of(
			Connection natsConnection, String subject, MessageConverter<String> messageConvertor) {
		NatsOutboundGatewaySpec spec = new NatsOutboundGatewaySpec();
		spec.init(new NatsTemplate(natsConnection, subject, messageConvertor), messageConvertor);
		return spec;
	}

	void init(
			@NonNull final NatsTemplate natsTemplate, @NonNull final MessageConverter messageConverter) {
		this.target = new NatsOutboundGateway(natsTemplate, messageConverter);
	}

	@Override
	protected NatsOutboundGatewaySpec id(final String idToSet) {
		this.target.setBeanName(idToSet);
		return super.id(idToSet);
	}

	/**
	 * Reply Channel to receive reply
	 *
	 * @param replyChannelName Name of reply Channel
	 * @return - TODO: Add description
	 */
	public NatsOutboundGatewaySpec replyChannelName(String replyChannelName) {
		this.target.setReplyChannelName(replyChannelName);
		return this;
	}

	/**
	 * Reply Channel to receive reply
	 *
	 * @param replyChannel Reply Channel Bean
	 * @return - TODO: Add description
	 */
	public NatsOutboundGatewaySpec replyChannel(MessageChannel replyChannel) {
		this.target.setReplyChannel(replyChannel);
		return this;
	}

	/**
	 * Error channel to receive error message
	 *
	 * @param errorChannelName Name of error Channel
	 * @return - TODO: Add description
	 */
	public NatsOutboundGatewaySpec errorChannelName(String errorChannelName) {
		this.target.setErrorChannelName(errorChannelName);
		return this;
	}

	/**
	 * Error channel to receive error message
	 *
	 * @param errorChannel Error Channel Bean
	 * @return - TODO: Add description
	 */
	public NatsOutboundGatewaySpec errorChannel(MessageChannel errorChannel) {
		this.target.setErrorChannel(errorChannel);
		return this;
	}

	/**
	 * A reply timeout to use.
	 *
	 * @param replyTimeout the replyTimeout.
	 * @return the spec.
	 * @see NatsOutboundGateway#setReplyTimeout(long)
	 */
	public NatsOutboundGatewaySpec replyTimeout(long replyTimeout) {
		this.target.setReplyTimeout(replyTimeout);
		return this;
	}

	/**
	 * @param extractRequestPayload the extractRequestPayload.
	 * @return the spec.
	 * @see NatsOutboundGateway#setExtractRequestPayload(boolean)
	 */
	public NatsOutboundGatewaySpec extractRequestPayload(boolean extractRequestPayload) {
		this.target.setExtractRequestPayload(extractRequestPayload);
		return this;
	}

	/**
	 * @param extractReplyPayload the extractReplyPayload.
	 * @return the spec.
	 * @see NatsOutboundGateway#setExtractReplyPayload(boolean)
	 */
	public NatsOutboundGatewaySpec extractReplyPayload(boolean extractReplyPayload) {
		this.target.setExtractReplyPayload(extractReplyPayload);
		return this;
	}
}
