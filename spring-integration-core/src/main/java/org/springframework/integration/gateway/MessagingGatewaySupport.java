/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.BridgeHandler;
import org.springframework.integration.history.HistoryWritingMessagePostProcessor;
import org.springframework.integration.history.TrackableComponent;
import org.springframework.integration.mapping.InboundMessageMapper;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.support.DefaultMessageBuilderFactory;
import org.springframework.integration.support.MessageBuilderFactory;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.util.Assert;

/**
 * A convenient base class for connecting application code to
 * {@link MessageChannel}s for sending, receiving, or request-reply operations.
 * Exposes setters for configuring request and reply {@link MessageChannel}s as
 * well as the timeout values for sending and receiving Messages.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class MessagingGatewaySupport extends AbstractEndpoint implements TrackableComponent {

	private static final long DEFAULT_TIMEOUT = 1000L;


	private volatile MessageChannel requestChannel;

	private volatile MessageChannel replyChannel;

	private volatile MessageChannel errorChannel;

	private volatile long replyTimeout = DEFAULT_TIMEOUT;

	@SuppressWarnings("rawtypes")
	private volatile InboundMessageMapper requestMapper = new DefaultRequestMapper();

	private final SimpleMessageConverter messageConverter = new SimpleMessageConverter();

	private final MessagingTemplate messagingTemplate;

	private final HistoryWritingMessagePostProcessor historyWritingPostProcessor = new HistoryWritingMessagePostProcessor();

	private volatile boolean initialized;

	private volatile AbstractEndpoint replyMessageCorrelator;

	private final Object replyMessageCorrelatorMonitor = new Object();


	public MessagingGatewaySupport() {
		MessagingTemplate template = new MessagingTemplate();
		template.setMessageConverter(this.messageConverter);
		template.setSendTimeout(DEFAULT_TIMEOUT);
		template.setReceiveTimeout(this.replyTimeout);
		this.messagingTemplate = template;
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
	 * Set the reply channel. If no reply channel is provided, this gateway will
	 * always use an anonymous, temporary channel for handling replies.
	 *
	 * @param replyChannel the channel from which reply messages will be received
	 */
	public void setReplyChannel(MessageChannel replyChannel) {
		this.replyChannel = replyChannel;
	}

	/**
	 * Set the error channel. If no error channel is provided, this gateway will
	 * propagate Exceptions to the caller. To completely suppress Exceptions, provide
	 * a reference to the "nullChannel" here.
	 *
	 * @param errorChannel The error channel.
	 */
	public void setErrorChannel(MessageChannel errorChannel) {
		this.errorChannel = errorChannel;
	}

	/**
	 * Set the timeout value for sending request messages. If not
	 * explicitly configured, the default is one second.
	 *
	 * @param requestTimeout the timeout value in milliseconds
	 */
	public void setRequestTimeout(long requestTimeout) {
		this.messagingTemplate.setSendTimeout(requestTimeout);
	}

	/**
	 * Set the timeout value for receiving reply messages. If not
	 * explicitly configured, the default is one second.
	 *
	 * @param replyTimeout the timeout value in milliseconds
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
		this.messagingTemplate.setReceiveTimeout(replyTimeout);
	}

	/**
	 * Provide an {@link InboundMessageMapper} for creating request Messages
	 * from any object passed in a send or sendAndReceive operation.
	 *
	 * @param requestMapper The request mapper.
	 */
	public void setRequestMapper(InboundMessageMapper<?> requestMapper) {
		requestMapper = (requestMapper != null) ? requestMapper : new DefaultRequestMapper();
		this.requestMapper = requestMapper;
		this.messageConverter.setInboundMessageMapper(requestMapper);
	}

	/**
	 * Provide an {@link OutboundMessageMapper} for mapping to objects from
	 * any reply Messages received in receive or sendAndReceive operations.
	 *
	 * @param replyMapper The reply mapper.
	 */
	public void setReplyMapper(OutboundMessageMapper<?> replyMapper) {
		this.messageConverter.setOutboundMessageMapper(replyMapper);
	}

	/**
	 * Specify whether this gateway should be tracked in the Message History
	 * of Messages that originate from its send or sendAndReceive operations.
	 */
	@Override
	public void setShouldTrack(boolean shouldTrack) {
		this.historyWritingPostProcessor.setShouldTrack(shouldTrack);
	}

	@Override
	public String getComponentType() {
		return "gateway";
	}

	@Override
	protected void onInit() throws Exception {
		this.historyWritingPostProcessor.setTrackableComponent(this);
		this.historyWritingPostProcessor.setMessageBuilderFactory(this.getMessageBuilderFactory());
		if (this.getBeanFactory() != null) {
			this.messagingTemplate.setBeanFactory(this.getBeanFactory());
			if (this.requestMapper instanceof DefaultRequestMapper) {
				((DefaultRequestMapper) this.requestMapper).setMessageBuilderFactory(this.getMessageBuilderFactory());
			}
			this.messageConverter.setBeanFactory(this.getBeanFactory());
		}
		this.initialized = true;
	}

	private void initializeIfNecessary() {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
	}

	protected void send(Object object) {
		this.initializeIfNecessary();
		Assert.notNull(object, "request must not be null");
		Assert.state(this.requestChannel != null,
				"send is not supported, because no request channel has been configured");
		try {
			this.messagingTemplate.convertAndSend(this.requestChannel, object, this.historyWritingPostProcessor);
		}
		catch (Exception e) {
			if (this.errorChannel != null) {
				this.messagingTemplate.send(this.errorChannel, new ErrorMessage(e));
			}
			else {
				this.rethrow(e, "failed to send message");
			}
		}
	}

	protected Object receive() {
		this.initializeIfNecessary();
		Assert.state(this.replyChannel != null && (this.replyChannel instanceof PollableChannel),
				"receive is not supported, because no pollable reply channel has been configured");
		return this.messagingTemplate.receiveAndConvert((PollableChannel) this.replyChannel, null);
	}

	protected Object sendAndReceive(Object object) {
		return this.doSendAndReceive(object, true);
	}

	protected Message<?> sendAndReceiveMessage(Object object) {
		return (Message<?>) this.doSendAndReceive(object, false);
	}

	@SuppressWarnings("unchecked")
	private Object doSendAndReceive(Object object, boolean shouldConvert) {
		this.initializeIfNecessary();
		Assert.notNull(object, "request must not be null");
		if (this.requestChannel == null) {
			throw new MessagingException("No request channel available. Cannot send request message.");
		}
		if (this.replyChannel != null && this.replyMessageCorrelator == null) {
			this.registerReplyMessageCorrelator();
		}
		Object reply = null;
		Throwable error = null;
		try {
			if (shouldConvert) {
				reply = this.messagingTemplate.convertSendAndReceive(this.requestChannel, object, null, this.historyWritingPostProcessor);
				if (reply instanceof Throwable) {
					error = (Throwable) reply;
				}
			}
			else {
				Message<?> requestMessage = (object instanceof Message<?>)
						? (Message<?>) object : this.requestMapper.toMessage(object);
				requestMessage = this.historyWritingPostProcessor.postProcessMessage(requestMessage);
				reply = this.messagingTemplate.sendAndReceive(this.requestChannel, requestMessage);
				if (reply instanceof ErrorMessage) {
					error = ((ErrorMessage) reply).getPayload();
				}
			}
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("failure occurred in gateway sendAndReceive: " + e.getMessage());
			}
			error = e;
		}

		if (error != null) {
			if (this.errorChannel != null) {
				Message<?> errorMessage = new ErrorMessage(error);
				Message<?> errorFlowReply = null;
				try {
					errorFlowReply = this.messagingTemplate.sendAndReceive(this.errorChannel, errorMessage);
				}
				catch (Exception errorFlowFailure) {
					throw new MessagingException(errorMessage, "failure occurred in error-handling flow", errorFlowFailure);
				}
				if (shouldConvert) {
					Object result = (errorFlowReply != null) ? errorFlowReply.getPayload() : null;
					if (result instanceof Throwable) {
						this.rethrow((Throwable) result, "error flow returned Exception");
					}
					return result;
				}
				if (errorFlowReply != null && errorFlowReply.getPayload() instanceof Throwable) {
					this.rethrow((Throwable) errorFlowReply.getPayload(), "error flow returned an Error Message");
				}
				return errorFlowReply;
			}
			else { // no errorChannel so we'll propagate
				this.rethrow(error, "gateway received checked Exception");
			}
		}
		return reply;
	}

	private void rethrow(Throwable t, String description) {
		if (t instanceof RuntimeException) {
			throw (RuntimeException) t;
		}
		throw new MessagingException(description, t);
	}

	private void registerReplyMessageCorrelator() {
		synchronized (this.replyMessageCorrelatorMonitor) {
			if (this.replyMessageCorrelator != null) {
				return;
			}
			AbstractEndpoint correlator = null;
			BridgeHandler handler = new BridgeHandler();
			if (this.getBeanFactory() != null) {
				handler.setBeanFactory(this.getBeanFactory());
			}
			handler.afterPropertiesSet();
			if (this.replyChannel instanceof SubscribableChannel) {
				correlator = new EventDrivenConsumer(
						(SubscribableChannel) this.replyChannel, handler);
			}
			else if (this.replyChannel instanceof PollableChannel) {
				PollingConsumer endpoint = new PollingConsumer(
						(PollableChannel) this.replyChannel, handler);
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


	private static class DefaultRequestMapper implements InboundMessageMapper<Object> {

		private volatile MessageBuilderFactory messageBuilderFactory = new DefaultMessageBuilderFactory();

		void setMessageBuilderFactory(MessageBuilderFactory messageBuilderFactory) {
			this.messageBuilderFactory = messageBuilderFactory;
		}

		@Override
		public Message<?> toMessage(Object object) throws Exception {
			if (object instanceof Message<?>) {
				return (Message<?>) object;
			}
			return (object != null) ? this.messageBuilderFactory.withPayload(object).build() : null;
		}
	}

}
