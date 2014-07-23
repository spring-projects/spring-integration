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

package org.springframework.integration.websocket.outbound;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.support.PassThruSubProtocolHandler;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.messaging.Message;
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
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketOutboundMessageHandler extends AbstractMessageHandler {

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

	private final boolean client;

	private volatile List<MessageConverter> messageConverters;

	private volatile boolean mergeWithDefaultConverters = false;

	public WebSocketOutboundMessageHandler(IntegrationWebSocketContainer webSocketContainer) {
		this(webSocketContainer, new SubProtocolHandlerRegistry(new PassThruSubProtocolHandler()));
	}

	public WebSocketOutboundMessageHandler(IntegrationWebSocketContainer webSocketContainer,
			SubProtocolHandlerRegistry protocolHandlerRegistry) {
		Assert.notNull(webSocketContainer, "'webSocketContainer' must not be null");
		Assert.notNull(protocolHandlerRegistry, "'protocolHandlerRegistry' must not be null");
		this.webSocketContainer = webSocketContainer;
		this.client = webSocketContainer instanceof ClientWebSocketContainer;
		this.protocolHandlerContainer = protocolHandlerRegistry;
		List<String> subProtocols = protocolHandlerRegistry.getSubProtocols();
		this.webSocketContainer.addSupportedProtocols(subProtocols.toArray(new String[subProtocols.size()]));
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

	@Override
	public String getComponentType() {
		return "websocket:outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
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
	protected void handleMessageInternal(Message<?> message) throws Exception {
		String sessionId = null;
		if (!this.client) {
			sessionId = this.protocolHandlerContainer.resolveSessionId(message);
			if (sessionId == null) {
				throw new IllegalArgumentException("The WebSocket 'sessionId' is required in the MessageHeaders");
			}
		}
		WebSocketSession session = this.webSocketContainer.getSession(sessionId);
		try {
			SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(message);
			headers.setLeaveMutable(true);
			headers.setMessageTypeIfNotSet(SimpMessageType.MESSAGE);
			Message<?> messageToSend = this.messageConverter.toMessage(message.getPayload(), headers.getMessageHeaders());
			this.protocolHandlerContainer.findProtocolHandler(session).handleMessageToClient(session, messageToSend);
		}
		catch (SessionLimitExceededException ex) {
			try {
				logger.error("Terminating session id '" + sessionId + "'", ex);
				this.webSocketContainer.closeSession(session, ex.getStatus());
			}
			catch (Exception secondException) {
				logger.error("Exception terminating session id '" + sessionId + "'", secondException);
			}
		}
	}

}
