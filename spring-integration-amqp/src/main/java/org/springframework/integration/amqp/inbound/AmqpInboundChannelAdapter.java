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
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.listener.exception.ListenerExecutionFailedException;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.StaticMessageHeaderAccessor;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.amqp.support.AmqpMessageHeaderErrorMessageStrategy;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.context.OrderlyShutdownCapable;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

import com.rabbitmq.client.Channel;

/**
 * Adapter that receives Messages from an AMQP Queue, converts them into
 * Spring Integration Messages, and sends the results to a Message Channel.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpInboundChannelAdapter extends MessageProducerSupport implements
		OrderlyShutdownCapable {

	private static final ThreadLocal<AttributeAccessor> attributesHolder = new ThreadLocal<AttributeAccessor>();

	private final AbstractMessageListenerContainer messageListenerContainer;

	private volatile MessageConverter messageConverter = new SimpleMessageConverter();

	private volatile AmqpHeaderMapper headerMapper = DefaultAmqpHeaderMapper.inboundMapper();

	private RetryTemplate retryTemplate;

	private RecoveryCallback<? extends Object> recoveryCallback;

	public AmqpInboundChannelAdapter(AbstractMessageListenerContainer listenerContainer) {
		Assert.notNull(listenerContainer, "listenerContainer must not be null");
		Assert.isNull(listenerContainer.getMessageListener(),
				"The listenerContainer provided to an AMQP inbound Channel Adapter " +
						"must not have a MessageListener configured since the adapter " +
						"configure its own listener implementation.");
		this.messageListenerContainer = listenerContainer;
		this.messageListenerContainer.setAutoStartup(false);
		setErrorMessageStrategy(new AmqpMessageHeaderErrorMessageStrategy());
	}


	public void setMessageConverter(MessageConverter messageConverter) {
		Assert.notNull(messageConverter, "messageConverter must not be null");
		this.messageConverter = messageConverter;
	}

	public void setHeaderMapper(AmqpHeaderMapper headerMapper) {
		Assert.notNull(headerMapper, "headerMapper must not be null");
		this.headerMapper = headerMapper;
	}

	/**
	 * Set a {@link RetryTemplate} to use for retrying a message delivery within the
	 * adapter. Unlike adding retry at the container level, this can be used with an
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
	 * Set a {@link RecoveryCallback} when using retry within the adapter.
	 * @param recoveryCallback the callback.
	 * @since 4.3.10
	 * @see #setRetryTemplate(RetryTemplate)
	 */
	public void setRecoveryCallback(RecoveryCallback<? extends Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}


	@Override
	public String getComponentType() {
		return "amqp:inbound-channel-adapter";
	}

	@Override
	protected void onInit() {
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
		super.onInit();
	}

	@Override
	protected void doStart() {
		this.messageListenerContainer.start();
	}

	@Override
	protected void doStop() {
		this.messageListenerContainer.stop();
	}

	@Override
	public int beforeShutdown() {
		this.stop();
		return 0;
	}

	@Override
	public int afterShutdown() {
		return 0;
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
			boolean retryDisabled = AmqpInboundChannelAdapter.this.retryTemplate == null;
			try {
				if (retryDisabled) {
					createAndSend(message, channel);
				}
				else {
					final org.springframework.messaging.Message<Object> toSend = createMessage(message, channel);
					AmqpInboundChannelAdapter.this.retryTemplate.execute(context -> {
								StaticMessageHeaderAccessor.getDeliveryAttempt(toSend).incrementAndGet();
								setAttributesIfNecessary(message, toSend);
								sendMessage(toSend);
								return null;
							},
							(RecoveryCallback<Object>) AmqpInboundChannelAdapter.this.recoveryCallback);
				}
			}
			catch (MessageConversionException e) {
				if (getErrorChannel() != null) {
					setAttributesIfNecessary(message, null);
					getMessagingTemplate().send(getErrorChannel(), buildErrorMessage(null,
							new ListenerExecutionFailedException("Message conversion failed", e, message)));
				}
				else {
					throw e;
				}
			}
			finally {
				if (retryDisabled) {
					attributesHolder.remove();
				}
			}
		}

		private void createAndSend(Message message, Channel channel) {
			org.springframework.messaging.Message<Object> messagingMessage = createMessage(message, channel);
			setAttributesIfNecessary(message, messagingMessage);
			sendMessage(messagingMessage);
		}

		private org.springframework.messaging.Message<Object> createMessage(Message message, Channel channel) {
			Object payload = AmqpInboundChannelAdapter.this.messageConverter.fromMessage(message);
			Map<String, Object> headers = AmqpInboundChannelAdapter.this.headerMapper
					.toHeadersFromRequest(message.getMessageProperties());
			if (AmqpInboundChannelAdapter.this.messageListenerContainer.getAcknowledgeMode()
					== AcknowledgeMode.MANUAL) {
				headers.put(AmqpHeaders.DELIVERY_TAG, message.getMessageProperties().getDeliveryTag());
				headers.put(AmqpHeaders.CHANNEL, channel);
			}
			if (AmqpInboundChannelAdapter.this.retryTemplate != null) {
				headers.put(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT, new AtomicInteger());
			}
			final org.springframework.messaging.Message<Object> messagingMessage = getMessageBuilderFactory()
					.withPayload(payload)
					.copyHeaders(headers)
					.build();
			return messagingMessage;
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			if (AmqpInboundChannelAdapter.this.recoveryCallback != null) {
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
