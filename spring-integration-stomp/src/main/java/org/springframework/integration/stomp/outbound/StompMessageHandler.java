/*
 * Copyright 2015-2020 the original author or authors.
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

package org.springframework.integration.stomp.outbound;

import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.event.StompExceptionEvent;
import org.springframework.integration.stomp.event.StompReceiptEvent;
import org.springframework.integration.stomp.support.StompHeaderMapper;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.ConnectionLostException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageHandler} implementation to send messages to STOMP destinations.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
public class StompMessageHandler extends AbstractMessageHandler
		implements ApplicationEventPublisherAware, ManageableLifecycle {

	private static final int DEFAULT_CONNECT_TIMEOUT = 3000;

	private final StompSessionHandler sessionHandler = new IntegrationOutboundStompSessionHandler();

	private final StompSessionManager stompSessionManager;

	private final Semaphore connectSemaphore = new Semaphore(0);

	private HeaderMapper<StompHeaders> headerMapper = new StompHeaderMapper();

	private Expression destinationExpression;

	private EvaluationContext evaluationContext;

	private ApplicationEventPublisher applicationEventPublisher;

	private long connectTimeout = DEFAULT_CONNECT_TIMEOUT;

	private volatile StompSession stompSession;

	private volatile Throwable transportError;

	private volatile boolean running;

	public StompMessageHandler(StompSessionManager stompSessionManager) {
		Assert.notNull(stompSessionManager, "'stompSessionManager' is required.");
		this.stompSessionManager = stompSessionManager;
	}

	public void setDestination(String destination) {
		Assert.hasText(destination, "'destination' must not be empty.");
		this.destinationExpression = new ValueExpression<>(destination);
	}

	public void setDestinationExpression(Expression destinationExpression) {
		Assert.notNull(destinationExpression, "'destinationExpression' must not be null.");
		this.destinationExpression = destinationExpression;
	}

	public void setHeaderMapper(HeaderMapper<StompHeaders> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	/**
	 * Specify the the timeout in milliseconds to wait for the STOMP session establishment.
	 * Must be greater than
	 * {@link org.springframework.integration.stomp.AbstractStompSessionManager#setRecoveryInterval(int)}.
	 * @param connectTimeout the timeout to use.
	 * @since 4.2.2
	 */
	public void setConnectTimeout(long connectTimeout) {
		this.connectTimeout = connectTimeout;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) {
		try {
			connectIfNecessary();
		}
		catch (Exception e) {
			throw new MessageDeliveryException(message, "The '" + this + "' could not deliver message.", e);
		}
		StompSession session = this.stompSession;

		StompHeaders stompHeaders = new StompHeaders();
		this.headerMapper.fromHeaders(message.getHeaders(), stompHeaders);
		if (stompHeaders.getDestination() == null) {
			Assert.state(this.destinationExpression != null,
					"One of 'destination' or 'destinationExpression' must be provided, " +
							"if message header doesn't supply 'destination' STOMP header.");
			String destination = this.destinationExpression.getValue(this.evaluationContext, message, String.class);
			stompHeaders.setDestination(destination);
		}

		final StompSession.Receiptable receiptable = session.send(stompHeaders, message.getPayload());
		if (receiptable.getReceiptId() != null) {
			final String destination = stompHeaders.getDestination();
			final ApplicationEventPublisher eventPublisher = this.applicationEventPublisher;
			if (eventPublisher != null) {
				receiptable.addReceiptTask(() -> {
					StompReceiptEvent event = new StompReceiptEvent(StompMessageHandler.this,
							destination, receiptable.getReceiptId(), StompCommand.SEND, false);
					event.setMessage(message);
					eventPublisher.publishEvent(event);
				});
			}
			receiptable.addReceiptLostTask(() -> {
				if (eventPublisher != null) {
					StompReceiptEvent event = new StompReceiptEvent(StompMessageHandler.this,
							destination, receiptable.getReceiptId(), StompCommand.SEND, true);
					event.setMessage(message);
					eventPublisher.publishEvent(event);
				}
				else {
					logger.error(() -> "The receipt [" + receiptable.getReceiptId() + "] is lost for [" +
							message + "] on destination [" + destination + "]");
				}
			});
		}
	}

	private void connectIfNecessary() throws InterruptedException {
		synchronized (this.connectSemaphore) {
			if (this.stompSession == null || !this.stompSessionManager.isConnected()) {
				this.stompSessionManager.disconnect(this.sessionHandler);
				this.stompSessionManager.connect(this.sessionHandler);
				if (!this.connectSemaphore.tryAcquire(this.connectTimeout, TimeUnit.MILLISECONDS)
						|| this.stompSession == null) {
					if (this.transportError != null) {
						if (this.transportError instanceof ConnectionLostException) {
							throw (ConnectionLostException) this.transportError;
						}
						else {
							throw new ConnectionLostException(this.transportError.getMessage());
						}
					}
					else {
						throw new ConnectionLostException("Failed to obtain StompSession during timeout: "
								+ this.connectTimeout);
					}
				}
			}
		}
	}

	@Override
	public void start() {
		this.running = true;
	}

	@Override
	public void stop() {
		this.running = false;
		this.stompSessionManager.disconnect(this.sessionHandler);
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	private class IntegrationOutboundStompSessionHandler extends StompSessionHandlerAdapter {

		IntegrationOutboundStompSessionHandler() {
		}

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			StompMessageHandler.this.transportError = null;
			StompMessageHandler.this.stompSession = session;
			StompMessageHandler.this.connectSemaphore.release();
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			Object thePayload = payload;
			if (thePayload == null) {
				thePayload = headers.getFirst(StompHeaderAccessor.STOMP_MESSAGE_HEADER);
			}
			if (thePayload != null) {
				Message<?> failedMessage = getMessageBuilderFactory().withPayload(thePayload)
						.copyHeaders(StompMessageHandler.this.headerMapper.toHeaders(headers))
						.build();
				MessagingException exception =
						new MessageDeliveryException(failedMessage, "STOMP frame handling error.");

				if (StompMessageHandler.this.applicationEventPublisher != null) {
					StompMessageHandler.this.applicationEventPublisher.publishEvent(
							new StompExceptionEvent(StompMessageHandler.this, exception));
				}
				else {
					logger.getLog().error(exception);
				}
			}
		}

		@Override
		public void handleException(StompSession session, @Nullable StompCommand command,
				StompHeaders headers, byte[] payload, Throwable exception) {

			Message<byte[]> failedMessage;
			if (command != null) {
				StompHeaderAccessor stompHeaderAccessor = StompHeaderAccessor.create(command, headers);
				failedMessage = MessageBuilder.createMessage(payload, stompHeaderAccessor.getMessageHeaders());
			}
			else {
				failedMessage =
						MessageBuilder.withPayload(payload)
								.copyHeaders(headers)
								.build();
			}
			logger.error(exception,
					() -> "The exception for session [" + session + "] on message [" + failedMessage + "]");
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			StompMessageHandler.this.transportError = exception;
			StompMessageHandler.this.stompSession = null;
		}

	}

}
