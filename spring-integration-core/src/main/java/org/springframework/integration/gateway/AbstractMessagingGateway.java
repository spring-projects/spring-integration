/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.ErrorMessage;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

/**
 * A convenient base class for connecting application code to
 * {@link MessageChannel}s for sending, receiving, or request-reply operations.
 * Exposes setters for configuring request and reply {@link MessageChannel}s as
 * well as the timeout values for sending and receiving Messages.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractMessagingGateway extends AbstractEndpoint {
	
	private volatile InboundMessageMapper<Throwable> exceptionMapper;

	private volatile MessageChannel requestChannel;

	private volatile MessageChannel replyChannel;

	private volatile long replyTimeout = 1000;

	private final MessagingTemplate messagingTemplate = new MessagingTemplate();

	private volatile boolean shouldThrowErrors = true;

	private volatile boolean initialized;

	private volatile AbstractEndpoint replyMessageCorrelator;

	private final Object replyMessageCorrelatorMonitor = new Object();

	@Override
	public String getComponentType(){
		return "gateway";
	}

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
		this.messagingTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is an indefinite timeout.
	 * 
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
		this.messagingTemplate.setReceiveTimeout(replyTimeout);
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

	/**
	 * Provide an {@link InboundMessageMapper} for creating a reply Message from
	 * an Exception that occurs downstream from this gateway. If no exceptionMapper
	 * is provided, then the {@link #shouldThrowErrors} property will dictate
	 * whether the error is rethrown or returned as an ErrorMessage.
	 */
	public void setExceptionMapper(InboundMessageMapper<Throwable> exceptionMapper) {
		this.exceptionMapper = exceptionMapper;
	}

	@Override
	protected void onInit() throws Exception {
		this.initialized = true;
	}

	private void initializeIfNecessary() {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
	}

	protected void send(Object object) {
		this.initializeIfNecessary();
		Assert.state(this.requestChannel != null,
				"send is not supported, because no request channel has been configured");
		Message<?> message = this.toMessage(object);
		Assert.notNull(message, "message must not be null");
		this.messagingTemplate.send(this.requestChannel, message);
	}

	protected Object receive() {
		this.initializeIfNecessary();
		Assert.state(this.replyChannel != null && (this.replyChannel instanceof PollableChannel),
				"receive is not supported, because no pollable reply channel has been configured");
		Message<?> message = this.messagingTemplate.receive((PollableChannel) this.replyChannel);
		try {
			return this.fromMessage(message);
		}
		catch (Exception e) {
			if (e instanceof RuntimeException) {
				throw (RuntimeException) e;
			}
			else throw new MessagingException(message, e);
		}
	}

	protected Object sendAndReceive(Object object) {
		return this.sendAndReceive(object, true);
	}

	protected Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) this.sendAndReceive(object, false);
	}

	Object sendAndReceive(Object object, boolean shouldMapMessage) {
		Message<?> request = this.toMessage(object);
		Message<?> reply = this.sendAndReceiveMessage(request);
		if (!shouldMapMessage) {
			return reply;
		}
		return this.fromMessage(reply);
	}

	private Message<?> sendAndReceiveMessage(Message<?> message) {
		this.initializeIfNecessary();
		Assert.notNull(message, "request message must not be null");
		if (this.requestChannel == null) {
			throw new MessageDeliveryException(message,
					"No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null && this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		Message<?> reply = null;
		Throwable error = null;
		try {
			reply = this.messagingTemplate.sendAndReceive(this.requestChannel, message);
			if (reply instanceof ErrorMessage) {
				error = ((ErrorMessage) reply).getPayload();
			}	
		}
		catch (Exception e) {
			e.printStackTrace();
			logger.warn("failure occurred in gateway sendAndReceive.", e);
			error = e;
		}
		
		if (error != null && this.exceptionMapper != null) {
			try {
				// create a reply message from the error
				return this.exceptionMapper.toMessage(error);
			}
			catch (Exception e2) {
				// ignore this, we'll handle the original error next 
			} 
		}
		if (error != null && this.shouldThrowErrors) {
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
			AbstractEndpoint correlator = null;
			MessageHandler handler = new BridgeHandler();
			if (this.replyChannel instanceof SubscribableChannel) {
				correlator = new EventDrivenConsumer(
						(SubscribableChannel) this.replyChannel, handler);
			}
			else if (this.replyChannel instanceof PollableChannel) {
				PollingConsumer endpoint = new PollingConsumer(
						(PollableChannel) this.replyChannel, handler);
				endpoint.setTrigger(new PeriodicTrigger(10));
				endpoint.setBeanFactory(this.getBeanFactory());
				endpoint.setReceiveTimeout(this.replyTimeout);
				endpoint.afterPropertiesSet();
				correlator = endpoint;
			}
			if (this.isRunning()) {
				correlator.start();
			}
			this.replyMessageCorrelator = correlator;
		}
	}

	protected Object fromMessage(Message<?> message) {
		return (message != null ? message.getPayload() : null);
	}

	protected Message<?> toMessage(Object object) {
		if (object instanceof Message<?>) {
			return (Message<?>) object;
		}
		else {
			return MessageBuilder.withPayload(object).build();
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStart() {
		if (this.replyMessageCorrelator != null) {
			replyMessageCorrelator.start();
		}
	}

	@Override // guarded by super#lifecycleLock
	protected void doStop() {
		if (this.replyMessageCorrelator != null) {
			this.replyMessageCorrelator.stop();
		}
	}
}
