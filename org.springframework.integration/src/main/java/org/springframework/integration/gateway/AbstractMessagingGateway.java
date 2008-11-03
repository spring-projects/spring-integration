/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.gateway;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.SubscribableChannel;
import org.springframework.integration.consumer.AbstractReplyProducingMessageHandler;
import org.springframework.integration.consumer.ReplyMessageHolder;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.endpoint.PollingConsumerEndpoint;
import org.springframework.integration.endpoint.SubscribingConsumerEndpoint;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;
import org.springframework.util.Assert;

/**
 * A convenient base class for connecting application code to
 * {@link MessageChannel}s for sending, receiving, or request-reply operations.
 * Exposes setters for configuring request and reply {@link MessageChannel}s as
 * well as the timeout values for sending and receiving Messages.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessagingGateway implements MessagingGateway, MessageEndpoint, TaskSchedulerAware, Lifecycle {

	private volatile MessageChannel requestChannel;

	private volatile MessageChannel replyChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private volatile boolean shouldThrowErrors = true;

	private volatile TaskScheduler taskScheduler;

	private volatile MessageEndpoint replyMessageCorrelator;

	private final Object replyMessageCorrelatorMonitor = new Object();

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	/**
	 * Set the request channel.
	 * 
	 * @param requestChannel the channel to which request messages will be sent
	 */
	public void setRequestChannel(MessageChannel requestChannel) {
		this.requestChannel = requestChannel;
	}

	/**
	 * Set the reply channel. If no reply channel is provided, this template will
	 * always use an anonymous, temporary channel for handling replies.
	 * 
	 * @param replyChannel the channel from which reply messages will be received
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.channelTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.channelTemplate.setReceiveTimeout(replyTimeout);
	}

	/**
	 * Specify whether the Throwable payload of a received {@link ErrorMessage}
	 * should be extracted and thrown from a send-and-receive operation.
	 * Otherwise, the ErrorMessage would be returned just like any other
	 * reply Message. The default is <code>true</code>.
	 */
	public void setShouldThrowErrors(boolean shouldThrowErrors) {
		this.shouldThrowErrors = shouldThrowErrors;
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	public void send(Object object) {
		Assert.state(this.requestChannel != null,
				"send is not supported, because no request channel has been configured");
		Message<?> message = this.toMessage(object);
		Assert.notNull(message, "message must not be null");
		if (!this.channelTemplate.send(message, this.requestChannel)) {
			throw new MessageDeliveryException(message, "failed to send Message to channel");
		}
	}

	public Object receive() {
		Assert.state(this.replyChannel != null && (this.replyChannel instanceof PollableChannel),
				"receive is not supported, because no pollable reply channel has been configured");
		Message<?> message = this.channelTemplate.receive((PollableChannel) this.replyChannel);
		return this.fromMessage(message);
	}

	public Object sendAndReceive(Object object) {
		return this.sendAndReceive(object, true);
	}

	public Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) this.sendAndReceive(object, false);
	}

	private Object sendAndReceive(Object object, boolean shouldMapMessage) {
		Message<?> request = this.toMessage(object);
		Message<?> reply = this.sendAndReceiveMessage(request);
		if (!shouldMapMessage) {
			return reply;
		}
		return this.fromMessage(reply);
	}

	private Message<?> sendAndReceiveMessage(Message<?> message) {
		Assert.notNull(message, "request message must not be null");
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null && this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		Message<?> reply = this.channelTemplate.sendAndReceive(message, this.requestChannel);
		if (reply != null && this.shouldThrowErrors && reply instanceof ErrorMessage) {
			Throwable error = ((ErrorMessage) reply).getPayload();
			if (error instanceof RuntimeException) {
				throw (RuntimeException) error;
			}
			throw new MessagingException("gateway received checked Exception", error);
		}
		return reply;
	}

	private void registerReplyMessageCorrelator() {
		synchronized (this.replyMessageCorrelatorMonitor) {
			if (this.replyMessageCorrelator != null) {
				return;
			}
			MessageEndpoint correlator = null;
			MessageHandler handler = new AbstractReplyProducingMessageHandler() {
				@Override
				protected void handleRequestMessage(Message<?> message, ReplyMessageHolder replyHolder) {
					replyHolder.set(message);
				}
			};
			if (this.replyChannel instanceof SubscribableChannel) {
				correlator = new SubscribingConsumerEndpoint(
						(SubscribableChannel) this.replyChannel, handler);
			}
			else if (this.replyChannel instanceof PollableChannel) {
				PollingConsumerEndpoint endpoint = new PollingConsumerEndpoint(
						(PollableChannel) this.replyChannel, handler);
				endpoint.setTaskScheduler(this.taskScheduler);
				endpoint.afterPropertiesSet();
				correlator = endpoint;
			}
			if (this.isRunning() && correlator instanceof Lifecycle) {
				((Lifecycle) correlator).start();
			}
			this.replyMessageCorrelator = correlator;
		}
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (this.replyMessageCorrelator != null && this.replyMessageCorrelator instanceof Lifecycle) {
				((Lifecycle) this.replyMessageCorrelator).start();
			}
			this.running = true;
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.replyMessageCorrelator != null && this.replyMessageCorrelator instanceof Lifecycle) {
				((Lifecycle) this.replyMessageCorrelator).stop();
			}
			this.running = false;
		}
	}

	/**
	 * Subclasses must implement this to map from an Object to a Message.
	 */
	protected abstract Message<?> toMessage(Object object);

	/**
	 * Subclasses must implement this to map from a Message to an Object.
	 */
	protected abstract Object fromMessage(Message<?> message);

}
