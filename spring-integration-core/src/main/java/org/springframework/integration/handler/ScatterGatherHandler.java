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
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
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

	public ScatterGatherHandler(MessageChannel scatterChannel, AggregatingMessageHandler gatherer) {
		Assert.notNull(scatterChannel);
		Assert.notNull(gatherer);
		this.scatterChannel = scatterChannel;
		this.gatherer = gatherer;
	}

	public ScatterGatherHandler(RecipientListRouter scatterRouter, AggregatingMessageHandler gatherer) {
		Assert.notNull(scatterRouter);
		Assert.notNull(gatherer);
		this.gatherer = gatherer;
		this.scatterChannel = new FixedSubscriberChannel(scatterRouter);
	}

	public void setGatherChannel(MessageChannel gatherChannel) {
		this.gatherChannel = gatherChannel;
	}

	public void setGatherTimeout(long gatherTimeout) {
		this.gatherTimeout = gatherTimeout;
	}

	@Override
	protected void onInit() {
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
				messagingTemplate.send(getOutputChannel(), message);
			}

		}));
	}

	@Override
	protected void handleMessageInternal(Message<?> requestMessage) throws Exception {
		Message<?> scatterMessage = requestMessage;
		if (this.gatherEndpoint == null) {
			scatterMessage = getMessageBuilderFactory().fromMessage(requestMessage)
					.setReplyChannel(this.gatherChannel)
					.build();
		}
		this.messagingTemplate.send(this.scatterChannel, scatterMessage);
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
