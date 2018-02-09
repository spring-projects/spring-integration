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

package org.springframework.integration.endpoint;

import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.AttributeAccessor;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.DefaultErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.integration.support.management.TrackableComponent;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A support class for producer endpoints that provides a setter for the
 * output channel and a convenience method for sending Messages.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class MessageProducerSupport extends AbstractEndpoint implements MessageProducer, TrackableComponent,
		SmartInitializingSingleton {

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private ErrorMessageStrategy errorMessageStrategy = new DefaultErrorMessageStrategy();

	private volatile MessageChannel outputChannel;

	private volatile String outputChannelName;

	private volatile MessageChannel errorChannel;

	private volatile String errorChannelName;

	private volatile boolean shouldTrack = false;

	protected MessageProducerSupport() {
		this.setPhase(Integer.MAX_VALUE / 2);
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Set the output channel name; overrides
	 * {@link #setOutputChannel(MessageChannel) outputChannel} if provided.
	 * @param outputChannelName the channel name.
	 * @since 4.3
	 */
	public void setOutputChannelName(String outputChannelName) {
		Assert.hasText(outputChannelName, "'outputChannelName' must not be null or empty");
		this.outputChannelName = outputChannelName;
	}

	@Override
	public MessageChannel getOutputChannel() {
		if (this.outputChannelName != null) {
			synchronized (this) {
				if (this.outputChannelName != null) {
					this.outputChannel = getChannelResolver().resolveDestination(this.outputChannelName);
					this.outputChannelName = null;
				}
			}
		}
		return this.outputChannel;
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	/**
	 * Set the error channel name. If no error channel is provided, this endpoint will
	 * propagate Exceptions to the message-driven source. To completely suppress
	 * Exceptions, provide a reference to the "nullChannel" here.
	 * @param errorChannelName The error channel bean name.
	 * @since 4.3
	 */
	public void setErrorChannelName(String errorChannelName) {
		Assert.hasText(errorChannelName, "'errorChannelName' must not be empty");
		this.errorChannelName = errorChannelName;
	}

	/**
	 * Return the error channel (if provided) to which error messages will
	 * be routed.
	 * @return the channel or null.
	 * @since 4.3
	 */
	public MessageChannel getErrorChannel() {
		if (this.errorChannelName != null) {
			synchronized (this) {
				if (this.errorChannelName != null) {
					this.errorChannel = getChannelResolver().resolveDestination(this.errorChannelName);
					this.errorChannelName = null;
				}
			}
		}
		return this.errorChannel;
	}

	/**
	 * Configure the default timeout value to use for send operations.
	 * May be overridden for individual messages.
	 * @param sendTimeout the send timeout in milliseconds
	 * @see MessagingTemplate#setSendTimeout
	 */
	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
	}

	/**
	 * Set an {@link ErrorMessageStrategy} to use to build an error message when a exception occurs.
	 * Default is the {@link DefaultErrorMessageStrategy}.
	 * @param errorMessageStrategy the {@link ErrorMessageStrategy}.
	 * @since 4.3.10
	 */
	public final void setErrorMessageStrategy(ErrorMessageStrategy errorMessageStrategy) {
		Assert.notNull(errorMessageStrategy, "'errorMessageStrategy' cannot be null");
		this.errorMessageStrategy = errorMessageStrategy;
	}

	protected MessagingTemplate getMessagingTemplate() {
		return this.messagingTemplate;
	}

	@Override
	public void afterSingletonsInstantiated() {
		Assert.state(this.outputChannel != null || StringUtils.hasText(this.outputChannelName),
				"'outputChannel' or 'outputChannelName' is required");
	}

	@Override
	protected void onInit() {
		try {
			super.onInit();
		}
		catch (Exception e) {
			throw new BeanInitializationException("Cannot initialize: " + this, e);
		}

		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(this.getBeanFactory());
		}

	}

	/**
	 * Takes no action by default. Subclasses may override this if they
	 * need lifecycle-managed behavior. Protected by 'lifecycleLock'.
	 */
	@Override
	protected void doStart() {
	}

	/**
	 * Takes no action by default. Subclasses may override this if they
	 * need lifecycle-managed behavior.
	 */
	@Override
	protected void doStop() {
	}

	protected void sendMessage(Message<?> message) {
		if (message == null) {
			throw new MessagingException("cannot send a null message");
		}
		if (this.shouldTrack) {
			message = MessageHistory.write(message, this, this.getMessageBuilderFactory());
		}
		try {
			this.messagingTemplate.send(getOutputChannel(), message);
		}
		catch (RuntimeException e) {
			if (!sendErrorMessageIfNecessary(message, e)) {
				throw e;
			}
		}
	}

	/**
	 * Send an error message based on the exception and message.
	 * @param message the message.
	 * @param exception the exception.
	 * @return true if the error channel is available and message sent.
	 * @since 4.3.10
	 */
	protected final boolean sendErrorMessageIfNecessary(Message<?> message, RuntimeException exception) {
		MessageChannel errorChannel = getErrorChannel();
		if (errorChannel != null) {
			this.messagingTemplate.send(errorChannel, buildErrorMessage(message, exception));
			return true;
		}
		return false;
	}

	/**
	 * Build an error message for the exception and message using the configured
	 * {@link ErrorMessageStrategy}.
	 * @param message the message.
	 * @param exception the exception.
	 * @return the error message.
	 * @since 4.3.10
	 */
	protected final ErrorMessage buildErrorMessage(Message<?> message, RuntimeException exception) {
		return this.errorMessageStrategy.buildErrorMessage(exception,
				getErrorMessageAttributes(message));
	}

	/**
	 * Populate an {@link AttributeAccessor} to be used when building an error message
	 * with the {@link #setErrorMessageStrategy(ErrorMessageStrategy)
	 * errorMessageStrategy}.
	 * @param message the message.
	 * @return the attributes.
	 * @since 4.3.10
	 */
	protected AttributeAccessor getErrorMessageAttributes(Message<?> message) {
		return ErrorMessageUtils.getAttributeAccessor(message, null);
	}

}
