/*
 * Copyright 2002-2018 the original author or authors.
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

package org.springframework.integration.amqp.inbound;

import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.gateway.MessagingGatewaySupport;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

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

	private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<AttributeAccessor>();

	private final AbstractMessageListenerContainer messageListenerContainer;

	private final AmqpTemplate amqpTemplate;

	private final boolean amqpTemplateExplicitlySet;

	private volatile MessageConverter amqpMessageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private Address defaultReplyTo;

	private RetryTemplate retryTemplate;

	private RecoveryCallback<? extends Object> recoveryCallback;

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
	 * @param recoveryCallback the callback.
	 * @since 4.3.10
	 * @see #setRetryTemplate(RetryTemplate)
	 */
	public void setRecoveryCallback(RecoveryCallback<? extends Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	@Override
	public String getComponentType() {
		return "amqp:inbound-gateway";
	}

	@Override
	protected void onInit() throws Exception {
		if (this.retryTemplate != null) {
			Assert.state(getErrorChannel() == null, "Cannot have an 'errorChannel' property when a 'RetryTemplate' is "
					+ "provided; use an 'ErrorMessageSendingRecoverer' in the 'recoveryCallback' property to "
					+ "send an error message when retries are exhausted");
		}
		Listener messageListener = new Listener();
		if (this.retryTemplate != null) {
			this.retryTemplate.registerListener(messageListener);
		}
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

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

	/**
	 * If there's a retry template, it will set the attributes holder via the listener. If
	 * there's no retry template, but there's an error channel, we create a new attributes
	 * holder here. If an attributes holder exists (by either method), we set the
	 * attributes for use by the {@link ErrorMessageStrategy}.
	 * @param amqpMessage the AMQP message to use.
	 * @param message the Spring Messaging message to use.
	 * @since 4.3.10
	 */
	private void setAttributesIfNecessary(Message amqpMessage, org.springframework.messaging.Message<?> message) {
		boolean needHolder = getErrorChannel() != null && this.retryTemplate == null;
		boolean needAttributes = needHolder || this.retryTemplate != null;
		if (needHolder) {
			attributesHolder.set(ErrorMessageUtils.getAttributeAccessor(null, null));
		}
		if (needAttributes) {
			AttributeAccessor attributes = attributesHolder.get();
			if (attributes != null) {
				attributes.setAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY, message);
				attributes.setAttribute(AmqpMessageHeaderErrorMessageStrategy.AMQP_RAW_MESSAGE, amqpMessage);
			}
		}
	}

	@Override
	protected AttributeAccessor getErrorMessageAttributes(org.springframework.messaging.Message<?> message) {
		AttributeAccessor attributes = attributesHolder.get();
		if (attributes == null) {
			return super.getErrorMessageAttributes(message);
		}
		else {
			return attributes;
		}
	}

	protected class Listener implements ChannelAwareMessageListener, RetryListener {

		@SuppressWarnings("unchecked")
		@Override
		public void onMessage(final Message message, final Channel channel) throws Exception {
			if (AmqpInboundGateway.this.retryTemplate == null) {
				try {
					org.springframework.messaging.Message<Object> converted = convert(message, channel);
					if (converted != null) {
						process(message, converted);
					}
				}
				finally {
					attributesHolder.remove();
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
			Map<String, Object> headers = null;
			Object payload = null;
			try {
				payload = AmqpInboundGateway.this.amqpMessageConverter.fromMessage(message);
				headers = AmqpInboundGateway.this.headerMapper.toHeadersFromRequest(message.getMessageProperties());
				if (AmqpInboundGateway.this.messageListenerContainer.getAcknowledgeMode() == AcknowledgeMode.MANUAL) {
					headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
					headers.put(AmqpHeaders.CHANNEL, channel);
				}
				if (AmqpInboundGateway.this.retryTemplate != null) {
					headers.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger());
				}
			}
			catch (RuntimeException e) {
				if (getErrorChannel() != null) {
					AmqpInboundGateway.this.messagingTemplate.send(getErrorChannel(), buildErrorMessage(null,
							new ListenerExecutionFailedException("Message conversion failed", e, message)));
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
			final org.springframework.messaging.Message<?> reply = sendAndReceiveMessage(messagingMessage);
			if (reply != null) {
				Address replyTo;
				String replyToProperty = message.getMessageProperties().getReplyTo();
				if (replyToProperty != null) {
					replyTo = new Address(replyToProperty);
				}
				else {
					replyTo = AmqpInboundGateway.this.defaultReplyTo;
				}

				MessagePostProcessor messagePostProcessor =
						message1 -> {
							MessageProperties messageProperties = message1.getMessageProperties();
							String contentEncoding = messageProperties.getContentEncoding();
							long contentLength = messageProperties.getContentLength();
							String contentType = messageProperties.getContentType();
							AmqpInboundGateway.this.headerMapper.fromHeadersToReply(reply.getHeaders(),
									messageProperties);
							// clear the replyTo from the original message since we are using it now
							messageProperties.setReplyTo(null);
							// reset the content-* properties as determined by the MessageConverter
							if (StringUtils.hasText(contentEncoding)) {
								messageProperties.setContentEncoding(contentEncoding);
							}
							messageProperties.setContentLength(contentLength);
							if (contentType != null) {
								messageProperties.setContentType(contentType);
							}
							return message1;
						};

				if (replyTo != null) {
					AmqpInboundGateway.this.amqpTemplate.convertAndSend(replyTo.getExchangeName(),
							replyTo.getRoutingKey(), reply.getPayload(), messagePostProcessor);
				}
				else {
					if (!AmqpInboundGateway.this.amqpTemplateExplicitlySet) {
						throw new IllegalStateException("There is no 'replyTo' message property " +
								"and the `defaultReplyTo` hasn't been configured.");
					}
					else {
						AmqpInboundGateway.this.amqpTemplate.convertAndSend(reply.getPayload(),
								messagePostProcessor);
					}
				}
			}
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			if (AmqpInboundGateway.this.recoveryCallback != null) {
				attributesHolder.set(context);
			}
			return true;
		}

		@Override
		public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {
			attributesHolder.remove();
		}

		@Override
		public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback,
				Throwable throwable) {
			// Empty
		}

	}

}
