/*
 * Copyright 2002-2016 the original author or authors.
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

import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.history.MessageHistory;
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

	public void setSendTimeout(long sendTimeout) {
		this.messagingTemplate.setSendTimeout(sendTimeout);
	}

	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.shouldTrack = shouldTrack;
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
			MessageChannel errorChannel = getErrorChannel();
			if (errorChannel != null) {
				this.messagingTemplate.send(errorChannel, new ErrorMessage(e));
			}
			else  {
				throw e;
			}
		}
	}

}
