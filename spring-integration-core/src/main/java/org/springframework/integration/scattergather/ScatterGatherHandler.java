/*
 * Copyright 2014-2018 the original author or authors.
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

package org.springframework.integration.scattergather;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.channel.HeaderChannelRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * The {@link MessageHandler} implementation for the
 * <a href="http://www.eaipatterns.com/BroadcastAggregate.html">Scatter-Gather</a> EIP pattern.
 *
 * @author Artem Bilan
 * @author Abdul Zaheer
 *
 * @since 4.1
 */
public class ScatterGatherHandler extends AbstractReplyProducingMessageHandler implements Lifecycle {

	private static final String GATHER_RESULT_CHANNEL = "gatherResultChannel";

	private final MessageChannel scatterChannel;

	private final MessageHandler gatherer;

	private MessageChannel gatherChannel;

	private long gatherTimeout = -1;

	private AbstractEndpoint gatherEndpoint;

	private HeaderChannelRegistry replyChannelRegistry;


	public ScatterGatherHandler(MessageChannel scatterChannel, MessageHandler gatherer) {
		Assert.notNull(scatterChannel, "'scatterChannel' must not be null");
		Assert.notNull(gatherer, "'gatherer' must not be null");
		Class<?> gathererClass = AopUtils.getTargetClass(gatherer);
		checkClass(gathererClass, "org.springframework.integration.aggregator.AggregatingMessageHandler", "gatherer");
		this.scatterChannel = scatterChannel;
		this.gatherer = gatherer;
	}

	public ScatterGatherHandler(MessageHandler scatterer, MessageHandler gatherer) {
		this(new FixedSubscriberChannel(scatterer), gatherer);
		Assert.notNull(scatterer, "'scatterer' must not be null");
		Class<?> scattererClass = AopUtils.getTargetClass(scatterer);
		checkClass(scattererClass, "org.springframework.integration.router.RecipientListRouter", "scatterer");
	}

	public void setGatherChannel(MessageChannel gatherChannel) {
		this.gatherChannel = gatherChannel;
	}

	public void setGatherTimeout(long gatherTimeout) {
		this.gatherTimeout = gatherTimeout;
	}

	@Override
	protected void doInit() {
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

		((MessageProducer) this.gatherer).setOutputChannel(new FixedSubscriberChannel(message -> {
			MessageHeaders headers = message.getHeaders();
			if (headers.containsKey(GATHER_RESULT_CHANNEL)) {
				Object gatherResultChannel = headers.get(GATHER_RESULT_CHANNEL);
				if (gatherResultChannel instanceof MessageChannel) {
					messagingTemplate.send((MessageChannel) gatherResultChannel, message);
				}
				else if (gatherResultChannel instanceof String) {
					messagingTemplate.send((String) gatherResultChannel, message);
				}
			}
			else {
				throw new MessageDeliveryException(message,
						"The 'gatherResultChannel' header is required to delivery gather result.");
			}
		}));

		this.replyChannelRegistry = getBeanFactory()
				.getBean(IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
						HeaderChannelRegistry.class);
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		PollableChannel gatherResultChannel = new QueueChannel();

		Object gatherResultChannelName = this.replyChannelRegistry.channelToChannelName(gatherResultChannel);

		Message<?> scatterMessage = getMessageBuilderFactory()
				.fromMessage(requestMessage)
				.setHeader(GATHER_RESULT_CHANNEL, gatherResultChannelName)
				.setReplyChannel(this.gatherChannel)
				.build();

		this.messagingTemplate.send(this.scatterChannel, scatterMessage);

		Message<?> gatherResult = gatherResultChannel.receive(this.gatherTimeout);
		if (gatherResult != null) {
			return getMessageBuilderFactory()
					.fromMessage(gatherResult)
					.removeHeader(GATHER_RESULT_CHANNEL)
					.setHeader(MessageHeaders.REPLY_CHANNEL, requestMessage.getHeaders().getReplyChannel())
					.build();
		}

		return null;
	}

	@Override
	public void start() {
		if (this.gatherEndpoint != null) {
			this.gatherEndpoint.start();
		}
	}

	@Override
	public void stop() {
		if (this.gatherEndpoint != null) {
			this.gatherEndpoint.stop();
		}
	}

	@Override
	public boolean isRunning() {
		return this.gatherEndpoint == null || this.gatherEndpoint.isRunning();
	}

	private void checkClass(Class<?> gathererClass, String className, String type) throws LinkageError {
		Class<?> clazz = null;
		try {
			clazz = ClassUtils.forName(className, getClass().getClassLoader());
		}
		catch (Exception e) {
		}
		Assert.isAssignable(clazz, gathererClass, "the '" + type + "' must be an " + className + " instance");
	}

}
