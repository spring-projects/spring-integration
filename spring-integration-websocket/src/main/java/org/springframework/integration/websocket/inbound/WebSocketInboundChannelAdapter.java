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

import java.util.List;

import org.springframework.context.Lifecycle;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.WebSocketListener;
import org.springframework.integration.websocket.support.SubProtocolHandlerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketInboundChannelAdapter extends MessageProducerSupport implements WebSocketListener {

	private final IntegrationWebSocketContainer webSocketContainer;

	private final SubProtocolHandlerContainer protocolHandlerContainer;

	private final MessageChannel subProtocolHandlerChannel;

	private volatile boolean active;

	public WebSocketInboundChannelAdapter(IntegrationWebSocketContainer webSocketContainer,
			SubProtocolHandlerContainer protocolHandlerContainer) {
		Assert.notNull(webSocketContainer, "'webSocketContainer' must not be null");
		Assert.notNull(protocolHandlerContainer, "'protocolHandlerContainer' must not be null");
		this.webSocketContainer = webSocketContainer;
		this.protocolHandlerContainer = protocolHandlerContainer;
		this.subProtocolHandlerChannel = new FixedSubscriberChannel(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				sendMessage(message);
			}

		});
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.webSocketContainer.setMessageListener(this);
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
