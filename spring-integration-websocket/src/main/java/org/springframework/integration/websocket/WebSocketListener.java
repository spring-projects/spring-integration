/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
 *
 * @since 4.1
 *
 * @see org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter
 */
public interface WebSocketListener extends SubProtocolCapable {

	/**
	 * Handle the received {@link WebSocketMessage}.
	 * @param session the WebSocket session
	 * @param message the WebSocket message
	 */
	void onMessage(WebSocketSession session, WebSocketMessage<?> message);

	/**
	 * Invoked after a {@link WebSocketSession} has started.
	 * @param session the WebSocket session
	 */
	void afterSessionStarted(WebSocketSession session);

	/**
	 * Invoked after a {@link WebSocketSession} has ended.
	 * @param session the WebSocket session
	 * @param closeStatus the reason why the session was closed
	 */
	void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus);

}
