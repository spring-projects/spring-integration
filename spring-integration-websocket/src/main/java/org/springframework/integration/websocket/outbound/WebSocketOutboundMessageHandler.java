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

import java.util.List;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.integration.websocket.IntegrationWebSocketContainer;
import org.springframework.integration.websocket.support.SubProtocolHandlerContainer;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.SessionLimitExceededException;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketOutboundMessageHandler extends AbstractMessageHandler {

	private final IntegrationWebSocketContainer webSocketContainer;

	private final SubProtocolHandlerContainer protocolHandlerContainer;

	private final boolean client;

	public WebSocketOutboundMessageHandler(IntegrationWebSocketContainer webSocketContainer,
			SubProtocolHandlerContainer protocolHandlerContainer) {
		Assert.notNull(webSocketContainer, "'webSocketContainer' must not be null");
		Assert.notNull(protocolHandlerContainer, "'protocolHandlerContainer' must not be null");
		this.webSocketContainer = webSocketContainer;
		this.client = webSocketContainer instanceof ClientWebSocketContainer;
		this.protocolHandlerContainer = protocolHandlerContainer;
		List<String> subProtocols = protocolHandlerContainer.getSubProtocols();
		this.webSocketContainer.addSupportedProtocols(subProtocols.toArray(new String[subProtocols.size()]));
	}

	@Override
	public String getComponentType() {
		return "websocket:outbound-channel-adapter";
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
			this.protocolHandlerContainer.findProtocolHandler(session).handleMessageToClient(session, message);
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
