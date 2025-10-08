/*
 * Copyright 2014-present the original author or authors.
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

package org.springframework.integration.scattergather;

import java.time.Duration;

import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.ReactiveStreamsSubscribableChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.ReactiveStreamsConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * The {@link MessageHandler} implementation for the
 * <a href="https://www.enterpriseintegrationpatterns.com/BroadcastAggregate.html">Scatter-Gather</a> EIP pattern.
 * <p>
 * When {@link #setAsync(boolean)} is {@code true}, the {@link ScatterGatherHandler} produces
 * a {@link Mono} as a reply based on the gather result.
 *
 * @author Artem Bilan
 * @author Abdul Zaheer
 * @author Jayadev Sirimamilla
 *
 * @since 4.1
 */
public class ScatterGatherHandler extends AbstractReplyProducingMessageHandler implements ManageableLifecycle {

	private static final String GATHER_RESULT_CHANNEL = "gatherResultChannel";

	private static final String ORIGINAL_ERROR_CHANNEL = "originalErrorChannel";

	private final MessageChannel scatterChannel;

	private final MessageHandler gatherer;

	@SuppressWarnings("NullAway.Init")
	private MessageChannel gatherChannel;

	private String errorChannelName = IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME;

	@SuppressWarnings("NullAway.Init")
	private Long gatherTimeout;

	private @Nullable AbstractEndpoint gatherEndpoint;

	public ScatterGatherHandler(MessageHandler scatterer, MessageHandler gatherer) {
		this(new FixedSubscriberChannel(scatterer), gatherer);
		Assert.notNull(scatterer, "'scatterer' must not be null");
		Class<?> scattererClass = AopUtils.getTargetClass(scatterer);
		checkClass(scattererClass, "org.springframework.integration.router.RecipientListRouter", "scatterer");
	}

	public ScatterGatherHandler(MessageChannel scatterChannel, MessageHandler gatherer) {
		Assert.notNull(scatterChannel, "'scatterChannel' must not be null");
		Assert.notNull(gatherer, "'gatherer' must not be null");
		Class<?> gathererClass = AopUtils.getTargetClass(gatherer);
		checkClass(gathererClass, "org.springframework.integration.aggregator.AggregatingMessageHandler", "gatherer");
		this.scatterChannel = scatterChannel;
		this.gatherer = gatherer;
	}

	public void setGatherChannel(MessageChannel gatherChannel) {
		this.gatherChannel = gatherChannel;
	}

	public void setGatherTimeout(long gatherTimeout) {
		this.gatherTimeout = gatherTimeout;
	}

	/**
	 * Specify a {@link MessageChannel} bean name for async error processing.
	 * Defaults to {@link IntegrationContextUtils#ERROR_CHANNEL_BEAN_NAME}.
	 * @param errorChannelName the {@link MessageChannel} bean name for async error processing.
	 * @since 5.1.3
	 */
	public void setErrorChannelName(String errorChannelName) {
		Assert.hasText(errorChannelName, "'errorChannelName' must not be empty.");
		this.errorChannelName = errorChannelName;
	}

	@Override
	public String getComponentType() {
		return "scatter-gather";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.scatter_gather;
	}

	@Override
	protected void doInit() {
		if (this.gatherTimeout == null) {
			this.gatherTimeout = getIntegrationProperties().getEndpointsDefaultTimeout();
		}

		BeanFactory beanFactory = getBeanFactory();
		if (this.gatherChannel == null) {
			this.gatherChannel =
					new FixedSubscriberChannel((message) ->
							this.gatherer.handleMessage(enhanceScatterReplyMessage(message)));
		}
		else {
			Assert.isInstanceOf(InterceptableChannel.class, this.gatherChannel,
					() -> "An injected 'gatherChannel' '" + this.gatherChannel +
							"' must be an 'InterceptableChannel' instance.");
			((InterceptableChannel) this.gatherChannel)
					.addInterceptor(0,
							new ChannelInterceptor() {

								@Override
								public Message<?> preSend(Message<?> message, MessageChannel channel) {
									return enhanceScatterReplyMessage(message);
								}

							});
			if (this.gatherChannel instanceof SubscribableChannel subscribableChannel) {
				this.gatherEndpoint = new EventDrivenConsumer(subscribableChannel, this.gatherer);
			}
			else if (this.gatherChannel instanceof PollableChannel pollableChannel) {
				this.gatherEndpoint = new PollingConsumer(pollableChannel, this.gatherer);
				((PollingConsumer) this.gatherEndpoint).setReceiveTimeout(this.gatherTimeout);
			}
			else if (this.gatherChannel instanceof ReactiveStreamsSubscribableChannel) {
				this.gatherEndpoint = new ReactiveStreamsConsumer(this.gatherChannel, this.gatherer);
			}
			else {
				throw new BeanInitializationException("Unsupported 'gatherChannel' type '" +
						this.gatherChannel.getClass() + "'. " +
						"'SubscribableChannel', 'PollableChannel' or 'ReactiveStreamsSubscribableChannel' " +
						"types are supported.");
			}
			this.gatherEndpoint.setBeanFactory(beanFactory);
			this.gatherEndpoint.afterPropertiesSet();
		}

		((MessageProducer) this.gatherer)
				.setOutputChannel(new FixedSubscriberChannel((message) -> {
					MessageHeaders headers = message.getHeaders();
					MessageChannel gatherResultChannel = headers.get(GATHER_RESULT_CHANNEL, MessageChannel.class);
					if (gatherResultChannel != null) {
						this.messagingTemplate.send(gatherResultChannel, message);
					}
					else {
						throw new MessageDeliveryException(message,
								"The 'gatherResultChannel' header is required to deliver the gather result.");
					}
				}));
	}

	private Message<?> enhanceScatterReplyMessage(Message<?> message) {
		MessageHeaders headers = message.getHeaders();
		return getMessageBuilderFactory()
				.fromMessage(message)
				.setHeader(MessageHeaders.ERROR_CHANNEL, headers.get(ORIGINAL_ERROR_CHANNEL))
				.build();
	}

	@Override
	protected @Nullable Object handleRequestMessage(Message<?> requestMessage) {
		MessageHeaders requestMessageHeaders = requestMessage.getHeaders();
		boolean async = isAsync();
		MessageChannel gatherResultChannel;
		Sinks.One<Message<?>> replyMono;

		if (async) {
			replyMono = Sinks.one();
			gatherResultChannel = (message, timeout) -> replyMono.tryEmitValue(message).isSuccess();
		}
		else {
			replyMono = null;
			gatherResultChannel = new QueueChannel();
		}

		Message<?> scatterMessage =
				getMessageBuilderFactory()
						.fromMessage(requestMessage)
						.setHeader(GATHER_RESULT_CHANNEL, gatherResultChannel)
						.setHeader(ORIGINAL_ERROR_CHANNEL, requestMessageHeaders.getErrorChannel())
						.setReplyChannel(this.gatherChannel)
						.setErrorChannelName(this.errorChannelName)
						.build();

		this.messagingTemplate.send(this.scatterChannel, scatterMessage);

		if (replyMono != null) {
			return replyMono.asMono()
					.map(this::replyFromGatherResult)
					.timeout(Duration.ofMillis(this.gatherTimeout), Mono.empty());
		}
		else {
			Message<?> gatherResult = ((PollableChannel) gatherResultChannel).receive(this.gatherTimeout);
			if (gatherResult != null) {
				return replyFromGatherResult(gatherResult);
			}
		}

		return null;
	}

	private AbstractIntegrationMessageBuilder<?> replyFromGatherResult(Message<?> gatherResult) {
		return getMessageBuilderFactory()
				.fromMessage(gatherResult)
				.removeHeaders(GATHER_RESULT_CHANNEL, ORIGINAL_ERROR_CHANNEL,
						MessageHeaders.REPLY_CHANNEL, MessageHeaders.ERROR_CHANNEL);
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

	private static void checkClass(Class<?> gathererClass, String className, String type) throws LinkageError {
		try {
			Class<?> clazz = ClassUtils.forName(className, ClassUtils.getDefaultClassLoader());
			Assert.isAssignable(clazz, gathererClass,
					() -> "the '" + type + "' must be an " + className + " " + "instance");
		}
		catch (ClassNotFoundException ex) {
			throw new IllegalStateException("The class for '" + className + "' cannot be loaded", ex);
		}
	}

}
