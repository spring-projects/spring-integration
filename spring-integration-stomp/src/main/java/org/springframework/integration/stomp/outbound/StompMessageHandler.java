/*
 * Copyright 2015 the original author or authors.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.springframework.integration.stomp.outbound;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.util.Assert;

/**
 * The {@link AbstractMessageHandler} implementation to send messages to STOMP destinations.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public class StompMessageHandler extends AbstractMessageHandler implements ApplicationEventPublisherAware, Lifecycle {

	private final StompSessionHandler sessionHandler = new IntegrationOutboundStompSessionHandler();

	private final StompSessionManager stompSessionManager;

	private volatile StompSession stompSession;

	private volatile boolean running;

	private volatile HeaderMapper<StompHeaders> headerMapper = new StompHeaderMapper();

	private Expression destinationExpression;

	private EvaluationContext evaluationContext;

	private ApplicationEventPublisher applicationEventPublisher;

	public StompMessageHandler(StompSessionManager stompSessionManager) {
		Assert.notNull(stompSessionManager, "'stompSessionManager' is required.");
		this.stompSessionManager = stompSessionManager;
	}

	public void setDestination(String destination) {
		Assert.hasText(destination, "'destination' must not be empty.");
		this.destinationExpression = new ValueExpression<String>(destination);
	}

	public void setDestinationExpression(Expression destinationExpression) {
		Assert.notNull(destinationExpression, "'destinationExpression' must not be null.");
		this.destinationExpression = destinationExpression;
	}

	public void setHeaderMapper(HeaderMapper<StompHeaders> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();

		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		}
	}

	@Override
	protected void handleMessageInternal(final Message<?> message) throws Exception {
		if (!this.isRunning()) {
			throw new MessageDeliveryException(message, "The StompMessageHandler [" + getComponentName() +
					"] hasn't been connected to StompSession. Check the state of [" + this.stompSessionManager + "]");
		}

		StompHeaders stompHeaders = new StompHeaders();
		this.headerMapper.fromHeaders(message.getHeaders(), stompHeaders);
		if (stompHeaders.getDestination() == null) {
			Assert.state(this.destinationExpression != null, "One of 'destination' or 'destinationExpression' must be" +
					" provided, if message header doesn't supply 'destination' STOMP header.");
			String destination = this.destinationExpression.getValue(this.evaluationContext, message, String.class);
			stompHeaders.setDestination(destination);
		}

		final StompSession.Receiptable receiptable = this.stompSession.send(stompHeaders, message.getPayload());
		if (receiptable.getReceiptId() != null) {
			final String destination = stompHeaders.getDestination();
			if (this.applicationEventPublisher != null) {
				receiptable.addReceiptTask(new Runnable() {

					@Override
					public void run() {
						StompReceiptEvent event = new StompReceiptEvent(StompMessageHandler.this,
								destination, receiptable.getReceiptId(), StompCommand.SEND, false);
						event.setMessage(message);
						applicationEventPublisher.publishEvent(event);
					}

				});
			}
			receiptable.addReceiptLostTask(new Runnable() {

				@Override
				public void run() {
					if (applicationEventPublisher != null) {
						StompReceiptEvent event = new StompReceiptEvent(StompMessageHandler.this,
								destination, receiptable.getReceiptId(), StompCommand.SEND, true);
						event.setMessage(message);
						applicationEventPublisher.publishEvent(event);
					}
					else {
						logger.error("The receipt [" + receiptable.getReceiptId() + "] is lost for [" +
								message + "] on destination [" + destination + "]");
					}
				}

			});
		}
	}

	@Override
	public void start() {
		this.stompSessionManager.connect(this.sessionHandler);
	}

	@Override
	public void stop() {
		this.stompSessionManager.disconnect(this.sessionHandler);
		this.running = false;
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}


	private class IntegrationOutboundStompSessionHandler extends StompSessionHandlerAdapter {

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			StompMessageHandler.this.stompSession = session;
			StompMessageHandler.this.running = true;
		}

		@Override
		public void handleFrame(StompHeaders headers, Object payload) {
			Object thePayload = payload;
			if (thePayload == null) {
				thePayload = headers.getFirst(StompHeaderAccessor.STOMP_MESSAGE_HEADER);
			}
			if (thePayload != null) {
				Message<?> failedMessage = getMessageBuilderFactory().withPayload(thePayload)
						.copyHeaders(headerMapper.toHeaders(headers))
						.build();
				MessagingException exception = new MessageDeliveryException(failedMessage,
						"STOMP frame handling error.");
				logger.error("STOMP frame handling error.", exception);
				if (applicationEventPublisher != null) {
					applicationEventPublisher.publishEvent(
							new StompExceptionEvent(StompMessageHandler.this, exception));
				}
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			logger.error("STOMP transport error for session: [" + session + "]", exception);
		}

	}

}
