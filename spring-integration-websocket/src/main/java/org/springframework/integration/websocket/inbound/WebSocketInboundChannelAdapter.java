/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.websocket.inbound;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationEventPublisherAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.json.JacksonPresent;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.integration.websocket.WebSocketListener;
import org.springframework.integration.websocket.event.ReceiptEvent;
import org.springframework.integration.websocket.support.PassThruSubProtocolHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.broker.AbstractBrokerMessageHandler;
import org.springframework.messaging.simp.broker.SimpleBrokerMessageHandler;
import org.springframework.messaging.simp.stomp.StompBrokerRelayMessageHandler;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
public class WebSocketInboundChannelAdapter extends MessageProducerSupport
		implements WebSocketListener, ApplicationEventPublisherAware {

	private static final byte[] EMPTY_PAYLOAD = new byte[0];

	private final List<MessageConverter> defaultConverters = new ArrayList<>(3);

	{
		this.defaultConverters.add(new StringMessageConverter());
		this.defaultConverters.add(new ByteArrayMessageConverter());
		if (JacksonPresent.isJackson2Present()) {
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
			converter.setContentTypeResolver(resolver);
			this.defaultConverters.add(converter);
		}
	}

	private final CompositeMessageConverter messageConverter = new CompositeMessageConverter(this.defaultConverters);

	private final IntegrationWebSocketContainer webSocketContainer;

	private final boolean server;

	private final SubProtocolHandlerRegistry subProtocolHandlerRegistry;

	private final MessageChannel subProtocolHandlerChannel;

	private final AtomicReference<Class<?>> payloadType = new AtomicReference<>(String.class);

	private ApplicationEventPublisher eventPublisher;

	private List<MessageConverter> messageConverters;

	private boolean mergeWithDefaultConverters = false;

	private boolean useBroker;

	private AbstractBrokerMessageHandler brokerHandler;

	public WebSocketInboundChannelAdapter(IntegrationWebSocketContainer webSocketContainer) {
		this(webSocketContainer, new SubProtocolHandlerRegistry(new PassThruSubProtocolHandler()));
	}

	public WebSocketInboundChannelAdapter(IntegrationWebSocketContainer webSocketContainer,
			SubProtocolHandlerRegistry protocolHandlerRegistry) {

		Assert.notNull(webSocketContainer, "'webSocketContainer' must not be null");
		Assert.notNull(protocolHandlerRegistry, "'protocolHandlerRegistry' must not be null");
		this.webSocketContainer = webSocketContainer;
		this.server = this.webSocketContainer instanceof ServerWebSocketContainer;
		this.subProtocolHandlerRegistry = protocolHandlerRegistry;
		this.subProtocolHandlerChannel =
				new FixedSubscriberChannel(message -> {
					try {
						handleMessageAndSend(message);
					}
					catch (Exception ex) {
						throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
								() -> "Failed to handle and process message in the ["
										+ WebSocketInboundChannelAdapter.this + ']', ex);
					}
				});
	}

	/**
	 * Set the message converters to use. These converters are used to convert the message to send for appropriate
	 * internal subProtocols type.
	 * @param messageConverters The message converters.
	 */
	public void setMessageConverters(List<MessageConverter> messageConverters) {
		Assert.noNullElements(messageConverters.toArray(), "'messageConverters' must not contain null entries");
		this.messageConverters = new ArrayList<>(messageConverters);
	}


	/**
	 * Flag which determines if the default converters should be available after
	 * custom converters.
	 * @param mergeWithDefaultConverters true to merge, false to replace.
	 */
	public void setMergeWithDefaultConverters(boolean mergeWithDefaultConverters) {
		this.mergeWithDefaultConverters = mergeWithDefaultConverters;
	}

	/**
	 * Set the type for target message payload to convert the WebSocket message body to.
	 * @param payloadType to convert inbound WebSocket message body
	 * @see CompositeMessageConverter
	 */
	public void setPayloadType(Class<?> payloadType) {
		Assert.notNull(payloadType, "'payloadType' must not be null");
		this.payloadType.set(payloadType);
	}

	/**
	 * Specify if this adapter should use an existing single {@link AbstractBrokerMessageHandler}
	 * bean for {@code non-MESSAGE} {@link org.springframework.web.socket.WebSocketMessage}s
	 * and to route messages with broker destinations.
	 * Since only single {@link AbstractBrokerMessageHandler} bean is allowed in the current
	 * application context, the algorithm to lookup the former by type, rather than applying
	 * the bean reference.
	 * This is used only on server side and is ignored from client side.
	 * @param useBroker the boolean flag.
	 */
	public void setUseBroker(boolean useBroker) {
		this.useBroker = useBroker;
	}

	@Override
	public void setApplicationEventPublisher(ApplicationEventPublisher applicationEventPublisher) {
		this.eventPublisher = applicationEventPublisher;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.webSocketContainer.setMessageListener(this);
		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			List<MessageConverter> converters = this.messageConverter.getConverters();
			if (this.mergeWithDefaultConverters) {
				ListIterator<MessageConverter> iterator =
						this.messageConverters.listIterator(this.messageConverters.size());
				while (iterator.hasPrevious()) {
					MessageConverter converter = iterator.previous();
					converters.add(0, converter);
				}
			}
			else {
				converters.clear();
				converters.addAll(this.messageConverters);
			}
		}
		if (this.server && this.useBroker) {
			Map<String, AbstractBrokerMessageHandler> brokers = getApplicationContext()
					.getBeansOfType(AbstractBrokerMessageHandler.class);
			for (AbstractBrokerMessageHandler broker : brokers.values()) {
				if (broker instanceof SimpleBrokerMessageHandler || broker instanceof StompBrokerRelayMessageHandler) {
					this.brokerHandler = broker;
					break;
				}
			}
			Assert.state(this.brokerHandler != null,
					"WebSocket Broker Relay isn't present in the application context; " +
							"it is required when 'useBroker = true'.");
		}
	}

	@Override
	public List<String> getSubProtocols() {
		return this.subProtocolHandlerRegistry.getSubProtocols();
	}

	@Override
	public void afterSessionStarted(WebSocketSession session) throws Exception { // NOSONAR Thrown from the delegate
		if (isActive()) {
			SubProtocolHandler protocolHandler = this.subProtocolHandlerRegistry.findProtocolHandler(session);
			protocolHandler.afterSessionStarted(session, this.subProtocolHandlerChannel);
			if (!this.server && protocolHandler instanceof StompSubProtocolHandler) {
				StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
				accessor.setSessionId(session.getId());
				accessor.setLeaveMutable(true);
				accessor.setAcceptVersion("1.1,1.2");

				Message<?> connectMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, accessor.getMessageHeaders());
				protocolHandler.handleMessageToClient(session, connectMessage);
			}
		}
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus)
			throws Exception { // NOSONAR Thrown from the delegate

		if (isActive()) {
			this.subProtocolHandlerRegistry.findProtocolHandler(session)
					.afterSessionEnded(session, closeStatus, this.subProtocolHandlerChannel);
		}
	}

	@Override
	public void onMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage)
			throws Exception { // NOSONAR Thrown from the delegate

		if (isActive()) {
			this.subProtocolHandlerRegistry.findProtocolHandler(session)
					.handleMessageFromClient(session, webSocketMessage, this.subProtocolHandlerChannel);
		}
	}

	@Override
	public String getComponentType() {
		return "websocket:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		if (this.webSocketContainer instanceof Lifecycle) {
			((Lifecycle) this.webSocketContainer).start();
		}
	}

	@Override
	protected void doStop() {
		if (this.webSocketContainer instanceof Lifecycle) {
			((Lifecycle) this.webSocketContainer).stop();
		}
	}

	public boolean isActive() {
		boolean active = super.isActive();
		if (!active) {
			logger.warn(() -> "MessageProducer '" + this + "' isn't started to accept WebSocket events.");
		}
		return active;
	}

	@SuppressWarnings("unchecked")
	private void handleMessageAndSend(final Message<?> message) {
		SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
		StompCommand stompCommand = (StompCommand) headerAccessor.getHeader("stompCommand");
		SimpMessageType messageType = headerAccessor.getMessageType();
		if (isProcessingTypeOrCommand(headerAccessor, stompCommand, messageType)) {
			if (SimpMessageType.CONNECT.equals(messageType)) {
				produceConnectAckMessage(message, headerAccessor);
			}
			else if (StompCommand.CONNECTED.equals(stompCommand)) {
				this.eventPublisher.publishEvent(new SessionConnectedEvent(this, (Message<byte[]>) message));
			}
			else if (StompCommand.RECEIPT.equals(stompCommand)) {
				this.eventPublisher.publishEvent(new ReceiptEvent(this, (Message<byte[]>) message));
			}
			else {
				produceMessage(message, headerAccessor);
			}
		}
		else {
			if (this.useBroker) {
				this.brokerHandler.handleMessage(message);
			}
			else {
				logger.debug(() -> "Messages with non 'SimpMessageType.MESSAGE' type are ignored for sending to the " +
						"'outputChannel'. They have to be emitted as 'ApplicationEvent's " +
						"from the 'SubProtocolHandler'. Or using 'AbstractBrokerMessageHandler'(useBroker = true) " +
						"from server side. Received message: " + message);
			}
		}
	}

	private boolean isProcessingTypeOrCommand(SimpMessageHeaderAccessor headerAccessor, StompCommand stompCommand,
			SimpMessageType messageType) {

		return (messageType == null // NOSONAR pretty simple logic
				|| SimpMessageType.MESSAGE.equals(messageType)
				|| (SimpMessageType.CONNECT.equals(messageType) && !this.useBroker)
				|| StompCommand.CONNECTED.equals(stompCommand)
				|| StompCommand.RECEIPT.equals(stompCommand))
				&& !checkDestinationPrefix(headerAccessor.getDestination());
	}

	private boolean checkDestinationPrefix(String destination) {
		if (this.useBroker) {
			Collection<String> destinationPrefixes = this.brokerHandler.getDestinationPrefixes();
			if ((destination == null) || CollectionUtils.isEmpty(destinationPrefixes)) {
				return false;
			}
			return destinationPrefixes.stream().anyMatch(destination::startsWith);
		}
		return false;
	}

	private void produceConnectAckMessage(Message<?> message, SimpMessageHeaderAccessor headerAccessor) {
		String sessionId = headerAccessor.getSessionId();
		SimpMessageHeaderAccessor connectAck = SimpMessageHeaderAccessor.create(SimpMessageType.CONNECT_ACK);
		connectAck.setSessionId(sessionId);
		connectAck.setHeader(SimpMessageHeaderAccessor.CONNECT_MESSAGE_HEADER, message);
		Message<byte[]> ackMessage = MessageBuilder.createMessage(EMPTY_PAYLOAD, connectAck.getMessageHeaders());
		WebSocketSession session = this.webSocketContainer.getSession(sessionId);
		try {
			this.subProtocolHandlerRegistry.findProtocolHandler(session).handleMessageToClient(session, ackMessage);
		}
		catch (Exception e) {
			throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
					() -> "Error sending connect ack message in the [" + this + ']', e);
		}
	}

	private void produceMessage(Message<?> message, SimpMessageHeaderAccessor headerAccessor) {
		headerAccessor.removeHeader(SimpMessageHeaderAccessor.NATIVE_HEADERS);
		Object payload = this.messageConverter.fromMessage(message, this.payloadType.get());
		Assert.state(payload != null,
				() -> "The message converter '" + this.messageConverter +
						"' produced no payload for message '" + message +
						"' and expected payload type: " + this.payloadType.get());
		Message<Object> messageToSend =
				getMessageBuilderFactory()
						.withPayload(payload)
						.copyHeaders(headerAccessor.toMap())
						.build();
		sendMessage(messageToSend);
	}

}
