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

package org.springframework.integration.websocket;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.SubProtocolCapable;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;

/**
 * A contract for handling incoming {@link WebSocketMessage}s messages as part of a higher
 * level protocol, referred to as "sub-protocol" in the WebSocket RFC specification.
 * <p>
 * Implementations of this interface can be configured on a
 * {@link IntegrationWebSocketContainer} which delegates messages and
 * {@link WebSocketSession} events to this implementation.
 *
 * @author Andy Wilkinson
 * @author Artem Bilan
 * @since 4.1
 * @see org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter
 */
public interface WebSocketListener extends SubProtocolCapable {

	/**
	 * Handle the received {@link WebSocketMessage}.
	 * @param session the WebSocket session
	 * @param message the WebSocket message
	 * @throws Exception the 'onMessage' Exception
	 */
	void onMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception;

	/**
	 * Invoked after a {@link WebSocketSession} has started.
	 * @param session the WebSocket session
	 * @throws Exception the 'afterSessionStarted' Exception
	 */
	void afterSessionStarted(WebSocketSession session) throws Exception;

	/**
	 * Invoked after a {@link WebSocketSession} has ended.
	 * @param session the WebSocket session
	 * @param closeStatus the reason why the session was closed
	 * @throws Exception the 'afterSessionEnded' Exception
	 */
	void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus) throws Exception;

}
