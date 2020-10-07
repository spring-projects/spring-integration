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

package org.springframework.integration.stomp.inbound;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.mapping.HeaderMapper;
import org.springframework.integration.stomp.StompSessionManager;
import org.springframework.integration.stomp.event.StompReceiptEvent;
import org.springframework.integration.stomp.support.StompHeaderMapper;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * The {@link MessageProducerSupport} for STOMP protocol to handle STOMP frames from
 * provided destination and send messages to the {@code outputChannel}.
 * <p>
 * Destinations can be added and removed at runtime.
 * <p>
 * The {@link StompReceiptEvent} is emitted for each {@code Subscribe STOMP frame}
 * if provided {@link StompSessionManager} supports {@code autoReceiptEnabled}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.2
 */
@ManagedResource
@IntegrationManagedResource
public class StompInboundChannelAdapter extends MessageProducerSupport implements ApplicationEventPublisherAware {

	private final StompSessionHandler stompSessionHandler = new IntegrationInboundStompSessionHandler();

	private final Set<String> destinations = new LinkedHashSet<>();

	private final StompSessionManager stompSessionManager;

	private final Map<String, StompSession.Subscription> subscriptions = new HashMap<>();

	private final Lock destinationLock = new ReentrantLock();

	private ApplicationEventPublisher applicationEventPublisher;

	private Class<?> payloadType = String.class;

	private HeaderMapper<StompHeaders> headerMapper = new StompHeaderMapper();

	private volatile StompSession stompSession;

	public StompInboundChannelAdapter(StompSessionManager stompSessionManager, String... destinations) {
		Assert.notNull(stompSessionManager, "'stompSessionManager' is required.");
		if (destinations != null) {
			for (String destination : destinations) {
				Assert.hasText(destination, "'destinations' must not have empty strings.");
				this.destinations.add(destination);
			}
		}
		this.stompSessionManager = stompSessionManager;
	}

	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null.");
		this.payloadType = payloadType;
	}

	public void setHeaderMapper(HeaderMapper<StompHeaders> headerMapper) {
		Assert.notNull(headerMapper, "'headerMapper' must not be null.");
		this.headerMapper = headerMapper;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.applicationEventPublisher = applicationEventPublisher;
	}

	@ManagedAttribute
	public String[] getDestinations() {
		this.destinationLock.lock();
		try {
			return this.destinations.toArray(new String[0]);
		}
		finally {
			this.destinationLock.unlock();
		}
	}

	/**
	 * Add a destination (or destinations) to the subscribed list and subscribe it.
	 * @param destination The destinations.
	 */
	@ManagedOperation
	public void addDestination(String... destination) {
		Assert.notNull(destination, "'destination' cannot be null");
		this.destinationLock.lock();
		try {
			Arrays.stream(destination)
					.filter(this.destinations::add)
					.forEach(d -> {
						if (this.logger.isDebugEnabled()) {
							logger.debug("Subscribe to destination '" + d + "'.");
						}
						subscribeDestination(d);
					});
		}
		finally {
			this.destinationLock.unlock();
		}
	}

	/**
	 * Remove a destination (or destinations) from the subscribed list and unsubscribe it.
	 * @param destination The destinations.
	 */
	@ManagedOperation
	public void removeDestination(String... destination) {
		Assert.notNull(destination, "'destination' cannot be null");
		this.destinationLock.lock();
		try {
			Arrays.stream(destination)
					.filter(this.destinations::remove)
					.forEach(d -> {
						if (this.logger.isDebugEnabled()) {
							logger.debug("Removed '" + d + "' from subscriptions.");
						}
						StompSession.Subscription subscription = this.subscriptions.get(d);
						if (subscription != null) {
							subscription.unsubscribe();
						}
						else {
							if (this.logger.isDebugEnabled()) {
								logger.debug("No subscription for destination '" + d + "'.");
							}
						}
					});
		}
		finally {
			this.destinationLock.unlock();
		}
	}

	@Override
	public String getComponentType() {
		return "stomp:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		if (this.stompSessionManager instanceof Lifecycle) {
			((Lifecycle) this.stompSessionManager).start();
		}
		this.stompSessionManager.connect(this.stompSessionHandler);
	}

	@Override
	protected void doStop() {
		this.stompSessionManager.disconnect(this.stompSessionHandler);
		try {
			for (StompSession.Subscription subscription : this.subscriptions.values()) {
				subscription.unsubscribe();
			}
		}
		catch (Exception ex) {
			logger.warn(ex, "The exception during unsubscribing.");
		}
		this.subscriptions.clear();
	}

	private void subscribeDestination(final String destination) {
		if (this.stompSession != null) {
			class FrameHandler implements StompFrameHandler {

				@Override
				public Type getPayloadType(StompHeaders headers) {
					return StompInboundChannelAdapter.this.payloadType;
				}

				@Override
				public void handleFrame(StompHeaders headers, @Nullable Object body) {
					Message<?> message;

					if (body == null) {
						logger.info("No body in STOMP frame: nothing to produce.");
						return;
					}
					else if (body instanceof Message) {
						message = (Message<?>) body;
					}
					else {
						message =
								getMessageBuilderFactory()
										.withPayload(body)
										.copyHeaders(
												StompInboundChannelAdapter.this.headerMapper.toHeaders(headers))
										.build();
					}
					sendMessage(message);
				}

			}
			final StompSession.Subscription subscription =
					this.stompSession.subscribe(destination, new FrameHandler());

			if (this.stompSessionManager.isAutoReceiptEnabled()) {
				final ApplicationEventPublisher eventPublisher = this.applicationEventPublisher;
				if (eventPublisher != null) {
					subscription.addReceiptTask(() -> {
						StompReceiptEvent event = new StompReceiptEvent(StompInboundChannelAdapter.this,
								destination, subscription.getReceiptId(), StompCommand.SUBSCRIBE, false);
						eventPublisher.publishEvent(event);
					});
				}
				subscription.addReceiptLostTask(() -> {
					if (eventPublisher != null) {
						StompReceiptEvent event = new StompReceiptEvent(StompInboundChannelAdapter.this,
								destination, subscription.getReceiptId(), StompCommand.SUBSCRIBE, true);
						eventPublisher.publishEvent(event);
					}
					else {
						logger.error("The receipt [" + subscription.getReceiptId() + "] is lost for [" +
								subscription.getSubscriptionId() + "] on destination [" + destination + "]");
					}
				});
			}
			this.subscriptions.put(destination, subscription);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("The StompInboundChannelAdapter [" + getComponentName() +
					"] ins't connected to StompSession. Check the state of [" + this.stompSessionManager + "]");
		}
	}

	private class IntegrationInboundStompSessionHandler extends StompSessionHandlerAdapter {

		@Override
		public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
			StompInboundChannelAdapter.this.stompSession = session;
			StompInboundChannelAdapter.this.destinations.forEach(StompInboundChannelAdapter.this::subscribeDestination);
		}

		@Override
		public void handleException(StompSession session, StompCommand command, StompHeaders headers, byte[] payload,
				Throwable exception) {

			String exceptionMessage = "STOMP Frame handling error in the [" + StompInboundChannelAdapter.this + ']';

			MessageChannel errorChannel = getErrorChannel();

			if (errorChannel != null) {
				StompHeaderAccessor headerAccessor = StompHeaderAccessor.create(command);
				headerAccessor.copyHeaders(StompInboundChannelAdapter.this.headerMapper.toHeaders(headers));
				Message<byte[]> failedMessage =
						MessageBuilder.createMessage(payload, headerAccessor.getMessageHeaders());

				Exception ex =
						IntegrationUtils.wrapInHandlingExceptionIfNecessary(failedMessage,
								() -> exceptionMessage, exception);

				getMessagingTemplate()
						.send(errorChannel, new ErrorMessage(ex));
			}
			else {
				logger.error(exception, exceptionMessage);
			}
		}

		@Override
		public void handleTransportError(StompSession session, Throwable exception) {
			StompInboundChannelAdapter.this.stompSession = null;
		}

	}

}
