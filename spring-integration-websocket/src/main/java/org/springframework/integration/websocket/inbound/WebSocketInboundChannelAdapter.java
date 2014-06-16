/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.websocket.inbound;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.atomic.AtomicReference;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.WebSocketListener;
import org.springframework.integration.websocket.support.PassThruSubProtocolHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.converter.ByteArrayMessageConverter;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.DefaultContentTypeResolver;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketInboundChannelAdapter extends MessageProducerSupport implements WebSocketListener {

	private final List<MessageConverter> defaultConverters = new ArrayList<MessageConverter>(3);

	{
		this.defaultConverters.add(new StringMessageConverter());
		this.defaultConverters.add(new ByteArrayMessageConverter());
		if (JacksonJsonUtils.isJackson2Present()) {
			DefaultContentTypeResolver resolver = new DefaultContentTypeResolver();
			resolver.setDefaultMimeType(MimeTypeUtils.APPLICATION_JSON);
			MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
			converter.setContentTypeResolver(resolver);
			this.defaultConverters.add(converter);
		}
	}

	private final CompositeMessageConverter messageConverter = new CompositeMessageConverter(this.defaultConverters);

	private final IntegrationWebSocketContainer webSocketContainer;

	private final SubProtocolHandlerRegistry protocolHandlerContainer;

	private final MessageChannel subProtocolHandlerChannel;

	private final AtomicReference<Class<?>> payloadType = new AtomicReference<Class<?>>(String.class);

	private volatile List<MessageConverter> messageConverters;

	private volatile boolean mergeWithDefaultConverters = false;

	private volatile boolean active;

	public WebSocketInboundChannelAdapter(IntegrationWebSocketContainer webSocketContainer) {
		this(webSocketContainer, new SubProtocolHandlerRegistry(new PassThruSubProtocolHandler()));
	}

	public WebSocketInboundChannelAdapter(IntegrationWebSocketContainer webSocketContainer,
			SubProtocolHandlerRegistry protocolHandlerRegistry) {
		Assert.notNull(webSocketContainer, "'webSocketContainer' must not be null");
		Assert.notNull(protocolHandlerRegistry, "'protocolHandlerRegistry' must not be null");
		this.webSocketContainer = webSocketContainer;
		this.protocolHandlerContainer = protocolHandlerRegistry;
		this.subProtocolHandlerChannel = new FixedSubscriberChannel(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				Object payload = WebSocketInboundChannelAdapter.this.messageConverter.fromMessage(message,
						WebSocketInboundChannelAdapter.this.payloadType.get());
				SimpMessageHeaderAccessor headerAccessor = SimpMessageHeaderAccessor.wrap(message);
				SimpMessageType messageType = headerAccessor.getMessageType();
				if (messageType == null || SimpMessageType.MESSAGE.equals(messageType)) {
					headerAccessor.removeHeader(SimpMessageHeaderAccessor.NATIVE_HEADERS);
					sendMessage(MessageBuilder.withPayload(payload).copyHeaders(headerAccessor.toMap()).build());
				}
				else {
				   if (logger.isDebugEnabled()) {
					   logger.debug("Messages with non 'SimpMessageType.MESSAGE' type are ignored for sending to the " +
							   "'outputChannel'. They have to be emitted as 'ApplicationEvent's " +
							   "from the 'SubProtocolHandler'. Received message: " + message);
				   }
				}
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
		this.messageConverters = new ArrayList<MessageConverter>(messageConverters);
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

	@Override
	protected void onInit() {
		super.onInit();
		this.webSocketContainer.setMessageListener(this);
		if (!CollectionUtils.isEmpty(this.messageConverters)) {
			List<MessageConverter> converters = this.messageConverter.getConverters();
			if (this.mergeWithDefaultConverters) {
				for (ListIterator<MessageConverter> iterator = this.messageConverters.listIterator(); iterator.hasPrevious(); ) {
					MessageConverter converter = iterator.previous();
					converters.add(0, converter);
				}
			}
			else {
				converters.clear();
				converters.addAll(this.messageConverters);
			}
		}
	}

	@Override
	public List<String> getSubProtocols() {
		return this.protocolHandlerContainer.getSubProtocols();
	}

	@Override
	public void afterSessionStarted(WebSocketSession session) throws Exception {
		if (isActive()) {
			this.protocolHandlerContainer.findProtocolHandler(session)
					.afterSessionStarted(session, this.subProtocolHandlerChannel);
		}
	}

	@Override
	public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus) throws Exception {
		if (isActive()) {
			this.protocolHandlerContainer.findProtocolHandler(session)
					.afterSessionEnded(session, closeStatus, this.subProtocolHandlerChannel);
		}
	}

	@Override
	public void onMessage(WebSocketSession session, WebSocketMessage<?> webSocketMessage) throws Exception {
		if (isActive()) {
			this.protocolHandlerContainer.findProtocolHandler(session)
					.handleMessageFromClient(session, webSocketMessage, this.subProtocolHandlerChannel);
		}
	}

	@Override
	public String getComponentType() {
		return "websocket:inbound-channel-adapter";
	}

	@Override
	protected void doStart() {
		this.active = true;
		if (this.webSocketContainer instanceof Lifecycle) {
			((Lifecycle) this.webSocketContainer).start();
		}
	}

	@Override
	protected void doStop() {
		this.active = false;
	}

	private boolean isActive() {
		if (!this.active) {
			logger.warn("MessageProducer '" + this + "'isn't started to accept WebSocket events");
		}
		return this.active;
	}

}
