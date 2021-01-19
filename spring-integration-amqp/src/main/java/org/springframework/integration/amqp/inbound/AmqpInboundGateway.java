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

package org.springframework.integration.amqp.inbound;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.batch.BatchingStrategy;
import org.springframework.amqp.rabbit.batch.SimpleBatchingStrategy;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.amqp.support.EndpointUtils;
import org.springframework.integration.amqp.support.MappingUtils;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetrySynchronizationManager;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import com.rabbitmq.client.Channel;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 * If a reply Message is received, it will be converted and sent back to
 * the AMQP 'replyTo'.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
public class AmqpInboundGateway extends MessagingGatewaySupport {

	private static final ThreadLocal<AttributeAccessor> ATTRIBUTES_HOLDER = new ThreadLocal<>();

	private final AbstractMessageListenerContainer messageListenerContainer;

	private final AmqpTemplate amqpTemplate;

	private final boolean amqpTemplateExplicitlySet;

	private MessageConverter amqpMessageConverter = new SimpleMessageConverter();

	private MessageConverter templateMessageConverter = this.amqpMessageConverter;

	private AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private Address defaultReplyTo;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<?> recoveryCallback;

	private MessageRecoverer messageRecoverer;

	private BatchingStrategy batchingStrategy = new SimpleBatchingStrategy(0, 0, 0L);

	private boolean bindSourceMessage;

	private boolean replyHeadersMappedLast;

	public AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer) {
		this(listenerContainer, new RabbitTemplate(listenerContainer.getConnectionFactory()), false);
	}

	/**
	 * Construct {@link AmqpInboundGateway} based on the provided {@link AbstractMessageListenerContainer}
	 * to receive request messages and {@link AmqpTemplate} to send replies.
	 * @param listenerContainer the {@link AbstractMessageListenerContainer} to receive AMQP messages.
	 * @param amqpTemplate the {@link AmqpTemplate} to send reply messages.
	 * @since 4.2
	 */
	public AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate) {
		this(listenerContainer, amqpTemplate, true);
	}

	private AmqpInboundGateway(AbstractMessageListenerContainer listenerContainer, AmqpTemplate amqpTemplate,
			boolean amqpTemplateExplicitlySet) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.notNull(amqpTemplate, "'amqpTemplate' must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to an AMQP inbound Gateway " +
						"must not have a MessageListener configured since " +
						"the adapter needs to configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		this.amqpTemplate = amqpTemplate;
		this.amqpTemplateExplicitlySet = amqpTemplateExplicitlySet;
		if (this.amqpTemplateExplicitlySet && this.amqpTemplate instanceof RabbitTemplate) {
			this.templateMessageConverter = ((RabbitTemplate) this.amqpTemplate).getMessageConverter();
		}
		setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
	}


	/**
	 * Specify the {@link MessageConverter} to convert request and reply to/from {@link Message}.
	 * If the {@link #amqpTemplate} is explicitly set, this {@link MessageConverter}
	 * isn't populated there. You must configure that external {@link #amqpTemplate}.
	 * @param messageConverter the {@link MessageConverter} to use.
	 */
	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "MessageConverter must not be null");
		this.amqpMessageConverter = messageConverter;
		if (!this.amqpTemplateExplicitlySet) {
			((RabbitTemplate) this.amqpTemplate).setMessageConverter(messageConverter);
			this.templateMessageConverter = messageConverter;
		}
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * The {@code defaultReplyTo} address with the form
	 * <pre class="code">
	 * (exchange)/(routingKey)
	 * </pre>
	 * or
	 * <pre class="code">
	 * (queueName)
	 * </pre>
	 * if the request message doesn't have a {@code replyTo} property.
	 * The second form uses the default exchange ("") and the queue name as
	 * the routing key.
	 * @param defaultReplyTo the default {@code replyTo} address to use.
	 * @since 4.2
	 * @see Address
	 */
	public void setDefaultReplyTo(String defaultReplyTo) {
		this.defaultReplyTo = new Address(defaultReplyTo);
	}

	/**
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * gateway. Unlike adding retry at the container level, this can be used with an
	 * {@code ErrorMessageSendingRecoverer} {@link RecoveryCallback} to publish to the
	 * error channel after retries are exhausted. You generally should not configure an
	 * error channel when using retry here, use a {@link RecoveryCallback} instead.
	 * @param retryTemplate the template.
	 * @since 4.3.10.
	 * @see #setRecoveryCallback(RecoveryCallback)
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	/**
	 * Set a {@link RecoveryCallback} when using retry within the gateway.
	 * Mutually exclusive with {@link #setMessageRecoverer(MessageRecoverer)}.
	 * @param recoveryCallback the callback.
	 * @since 4.3.10
	 * @see #setRetryTemplate(RetryTemplate)
	 */
	public void setRecoveryCallback(RecoveryCallback<? extends Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * Configure a {@link MessageRecoverer} for retry operations.
	 * A more AMQP-specific convenience instead of {@link #setRecoveryCallback(RecoveryCallback)}.
	 * @param messageRecoverer the {@link MessageRecoverer} to use.
	 * @since 5.5
	 */
	public void setMessageRecoverer(MessageRecoverer messageRecoverer) {
		this.messageRecoverer = messageRecoverer;
	}

	/**
	 * Set a batching strategy to use when de-batching messages.
	 * Default is {@link SimpleBatchingStrategy}.
	 * @param batchingStrategy the strategy.
	 * @since 5.2
	 */
	public void setBatchingStrategy(BatchingStrategy batchingStrategy) {
		Assert.notNull(batchingStrategy, "'batchingStrategy' cannot be null");
		this.batchingStrategy = batchingStrategy;
	}

	/**
	 * Set to true to bind the source message in the header named
	 * {@link IntegrationMessageHeaderAccessor#SOURCE_DATA}.
	 * @param bindSourceMessage true to bind.
	 * @since 5.1.6
	 */
	public void setBindSourceMessage(boolean bindSourceMessage) {
		this.bindSourceMessage = bindSourceMessage;
	}

	/**
	 * When mapping headers for the outbound (reply) message, determine whether the headers are
	 * mapped before the message is converted, or afterwards. This only affects headers
	 * that might be added by the message converter. When false, the converter's headers
	 * win; when true, any headers added by the converter will be overridden (if the
	 * source message has a header that maps to those headers). You might wish to set this
	 * to true, for example, when using a
	 * {@link org.springframework.amqp.support.converter.SimpleMessageConverter} with a
	 * String payload that contains json; the converter will set the content type to
	 * {@code text/plain} which can be overridden to {@code application/json} by setting
	 * the {@link AmqpHeaders#CONTENT_TYPE} message header. Default: false.
	 * @param replyHeadersMappedLast true if reply headers are mapped after conversion.
	 * @since 5.1.9
	 */
	public void setReplyHeadersMappedLast(boolean replyHeadersMappedLast) {
		this.replyHeadersMappedLast = replyHeadersMappedLast;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-gateway";
	}

	@Override
	protected void onInit() {
		if (this.retryTemplate != null) {
			Assert.state(getErrorChannel() == null, "Cannot have an 'errorChannel' property when a 'RetryTemplate' is "
					+ "provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to "
					+ "send an error message when retries are exhausted");
			setupRecoveryCallbackIfAny();
		}
		Listener messageListener = new Listener();
		this.messageListenerContainer.setMessageListener(messageListener);
		this.messageListenerContainer.afterPropertiesSet();
		if (!this.amqpTemplateExplicitlySet) {
			((RabbitTemplate) this.amqpTemplate).afterPropertiesSet();
		}
		super.onInit();
		if (this.retryTemplate != null && getErrorChannel() != null) {
			logger.warn("Usually, when using a RetryTemplate you should use an ErrorMessageSendingRecoverer and not "
					+ "provide an errorChannel. Using an errorChannel could defeat retry and will receive an error "
					+ "message for each delivery attempt.");
		}
	}

	private void setupRecoveryCallbackIfAny() {
		Assert.state(this.recoveryCallback == null || this.messageRecoverer == null,
				"Only one of 'recoveryCallback' or 'messageRecoverer' may be provided, but not both");
		if (this.messageRecoverer != null) {
				this.recoveryCallback =
						context -> {
							Message messageToRecover =
									(Message) RetrySynchronizationManager.getContext()
											.getAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE);
							this.messageRecoverer.recover(messageToRecover, context.getLastThrowable());
							return null;
						};
		}
	}

	@Override
	protected void doStart() {
		super.doStart();
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		super.doStop();
		this.messageListenerContainer.stop();
	}

	/**
	 * If there's a retry template, it will set the attributes holder via the listener. If
	 * there's no retry template, but there's an error channel, we create a new attributes
	 * holder here. If an attributes holder exists (by either method), we set the
	 * attributes for use by the
	 * {@link org.springframework.integration.support.ErrorMessageStrategy}.
	 * @param amqpMessage the AMQP message to use.
	 * @param message the Spring Messaging message to use.
	 * @since 4.3.10
	 */
	private void setAttributesIfNecessary(Message amqpMessage, org.springframework.messaging.Message<?> message) {
		boolean needHolder = getErrorChannel() != null && this.retryTemplate == null;
		boolean needAttributes = needHolder || this.retryTemplate != null;
		if (needHolder) {
			ATTRIBUTES_HOLDER.set(ErrorMessageUtils.getAttributeAccessor(null, null));
		}
		if (needAttributes) {
			AttributeAccessor attributes = this.retryTemplate != null
					? RetrySynchronizationManager.getContext()
					: ATTRIBUTES_HOLDER.get();
			if (attributes != null) {
				attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
				attributes.setAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE, amqpMessage);
			}
		}
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(org.springframework.messaging.Message<?> message) {
		AttributeAccessor attributes = ATTRIBUTES_HOLDER.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	protected class Listener implements ChannelAwareMessageListener {

		@SuppressWarnings("unchecked")
		@Override
		public void onMessage(final Message message, final Channel channel) {
			if (AmqpInboundGateway.this.retryTemplate == null) {
				try {
					org.springframework.messaging.Message<Object> converted = convert(message, channel);
					if (converted != null) {
						process(message, converted);
					}
				}
				finally {
					ATTRIBUTES_HOLDER.remove();
				}
			}
			else {
				org.springframework.messaging.Message<Object> converted = convert(message, channel);
				if (converted != null) {
					AmqpInboundGateway.this.retryTemplate.execute(context -> {
								StaticMessageHeaderAccessor.getDeliveryAttempt(converted).incrementAndGet();
								process(message, converted);
								return null;
							},
							(RecoveryCallback<Object>) AmqpInboundGateway.this.recoveryCallback);
				}
			}
		}

		private org.springframework.messaging.Message<Object> convert(Message message, Channel channel) {
			Map<String, Object> headers;
			Object payload;
			boolean isManualAck =
					AmqpInboundGateway.this.messageListenerContainer.getAcknowledgeMode() == AcknowledgeMode.MANUAL;
			try {
				if (AmqpInboundGateway.this.batchingStrategy.canDebatch(message.getMessageProperties())) {
					List<Object> payloads = new ArrayList<>();
					AmqpInboundGateway.this.batchingStrategy.deBatch(message, fragment -> payloads
							.add(AmqpInboundGateway.this.amqpMessageConverter.fromMessage(fragment)));
					payload = payloads;
				}
				else {
					payload = AmqpInboundGateway.this.amqpMessageConverter.fromMessage(message);
				}
				headers = AmqpInboundGateway.this.headerMapper.toHeadersFromRequest(message.getMessageProperties());
				if (isManualAck) {
					headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
					headers.put(AmqpHeaders.CHANNEL, channel);
				}
				if (AmqpInboundGateway.this.retryTemplate != null) {
					headers.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger());
				}
				if (AmqpInboundGateway.this.bindSourceMessage) {
					headers.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, message);
				}
			}
			catch (RuntimeException e) {
				MessageChannel errorChannel = getErrorChannel();
				if (errorChannel != null) {
					setAttributesIfNecessary(message, null);
					AmqpInboundGateway.this.messagingTemplate.send(errorChannel,
							buildErrorMessage(null,
									EndpointUtils.errorMessagePayload(message, channel, isManualAck, e)));
				}
				else {
					throw e;
				}
				return null;
			}
			return getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
		}

		private void process(Message message, org.springframework.messaging.Message<Object> messagingMessage) {
			setAttributesIfNecessary(message, messagingMessage);
			org.springframework.messaging.Message<?> reply = sendAndReceiveMessage(messagingMessage);
			if (reply != null) {
				Address replyTo;
				String replyToProperty = message.getMessageProperties().getReplyTo();
				if (replyToProperty != null) {
					replyTo = new Address(replyToProperty);
				}
				else {
					replyTo = AmqpInboundGateway.this.defaultReplyTo;
				}

				org.springframework.amqp.core.Message amqpMessage =
						MappingUtils.mapReplyMessage(reply, AmqpInboundGateway.this.templateMessageConverter,
								AmqpInboundGateway.this.headerMapper,
								message.getMessageProperties().getReceivedDeliveryMode(),
								AmqpInboundGateway.this.replyHeadersMappedLast);

				if (replyTo != null) {
					AmqpInboundGateway.this.amqpTemplate.send(replyTo.getExchangeName(), replyTo.getRoutingKey(),
							amqpMessage);
				}
				else {
					if (!AmqpInboundGateway.this.amqpTemplateExplicitlySet) {
						throw new IllegalStateException("There is no 'replyTo' message property " +
								"and the `defaultReplyTo` hasn't been configured.");
					}
					else {
						AmqpInboundGateway.this.amqpTemplate.send(amqpMessage);
					}
				}
			}
		}

	}

}
