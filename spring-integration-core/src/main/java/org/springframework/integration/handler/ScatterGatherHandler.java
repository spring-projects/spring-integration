/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import org.springframework.context.Lifecycle;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;

/**
 * The {@link MessageHandler} implementation for the
 * <a href="http://www.eaipatterns.com/BroadcastAggregate.html">Scatter-Gather</a> EIP pattern.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class ScatterGatherHandler extends AbstractMessageProducingHandler implements Lifecycle {

	private final MessageChannel scatterChannel;

	private final AggregatingMessageHandler gatherer;

	private MessageChannel gatherChannel;

	private long gatherTimeout = -1;

	private AbstractEndpoint gatherEndpoint;

	private HeaderChannelRegistry replyChannelRegistry;


	public ScatterGatherHandler(MessageChannel scatterChannel, AggregatingMessageHandler gatherer) {
		Assert.notNull(scatterChannel);
		Assert.notNull(gatherer);
		this.scatterChannel = scatterChannel;
		this.gatherer = gatherer;
	}

	public ScatterGatherHandler(RecipientListRouter scatterer, AggregatingMessageHandler gatherer) {
		Assert.notNull(scatterer);
		Assert.notNull(gatherer);
		this.gatherer = gatherer;
		this.scatterChannel = new FixedSubscriberChannel(scatterer);
	}

	public void setGatherChannel(MessageChannel gatherChannel) {
		this.gatherChannel = gatherChannel;
	}

	public void setGatherTimeout(long gatherTimeout) {
		this.gatherTimeout = gatherTimeout;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (this.gatherChannel == null) {
			this.gatherChannel = new FixedSubscriberChannel(this.gatherer);
		}
		else {
			if (this.gatherChannel instanceof SubscribableChannel) {
				this.gatherEndpoint = new EventDrivenConsumer((SubscribableChannel) this.gatherChannel, this.gatherer);
			}
			else if (this.gatherChannel instanceof PollableChannel) {
				this.gatherEndpoint = new PollingConsumer((PollableChannel) this.gatherChannel, this.gatherer);
				((PollingConsumer) this.gatherEndpoint).setReceiveTimeout(this.gatherTimeout);
			}
			else {
				throw new MessagingException("Unsupported 'replyChannel' type [" + this.gatherChannel.getClass() + "]."
						+ "SubscribableChannel or PollableChannel type are supported.");
			}
			this.gatherEndpoint.setBeanFactory(this.getBeanFactory());
			this.gatherEndpoint.afterPropertiesSet();
		}
		this.gatherer.setOutputChannel(new FixedSubscriberChannel(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				sendReply(message);
			}

		}));

		this.replyChannelRegistry = getBeanFactory()
				.getBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
						HeaderChannelRegistry.class);
	}

	@Override
	protected void handleMessageInternal(Message<?> requestMessage) throws Exception {
		Message<?> scatterMessage = requestMessage;
		AbstractIntegrationMessageBuilder<?> scatterMessageBuilder = null;

		if (requestMessage.getHeaders().getReplyChannel() != null) {
			Object originalReplyChannelName = this.replyChannelRegistry
					.channelToChannelName(requestMessage.getHeaders().getReplyChannel());
			scatterMessageBuilder = getMessageBuilderFactory().fromMessage(requestMessage)
					.setHeader("originalReplyChannel", originalReplyChannelName);
		}

		if (this.gatherEndpoint == null) {
			if (scatterMessageBuilder == null) {
				scatterMessageBuilder = getMessageBuilderFactory().fromMessage(requestMessage);
			}
			scatterMessage = scatterMessageBuilder.setReplyChannel(this.gatherChannel).build();
		}

		this.messagingTemplate.send(this.scatterChannel, scatterMessage);
	}

	private void sendReply(Message message) {
		Object replyChannelHeader = message.getHeaders().get("originalReplyChannel");

		Object replyChannel = getOutputChannel();
		if (replyChannel == null) {
			replyChannel = replyChannelHeader;
		}
		if (replyChannel instanceof MessageChannel) {
			this.messagingTemplate.send((MessageChannel) replyChannel, message);
		}
		else if (replyChannel instanceof String) {
			this.messagingTemplate.send((String) replyChannel, message);
		}
		else {
			throw new MessageDeliveryException(message,
					"a non-null reply channel value of type MessageChannel or String is required");
		}
	}

	@Override
	public void start() {
		if (this.gatherEndpoint != null) {
			gatherEndpoint.start();
		}
	}

	@Override
	public void stop() {
		if (this.gatherEndpoint != null) {
			gatherEndpoint.start();
		}
	}

	@Override
	public boolean isRunning() {
		return this.gatherEndpoint == null || this.gatherEndpoint.isRunning();
	}

}
