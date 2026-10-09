/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.Assert;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/**
 * The {@link WebSocketStompClient} based {@link AbstractStompSessionManager} implementation.
 *
 * @author Artem Bilan
 * @author Sean Mills
 *
 * @since 4.2
 *
 * @see WebSocketStompClient
 */
public class WebSocketStompSessionManager extends AbstractStompSessionManager {

	private final String url;

	private final Object[] uriVariables;

	private volatile @Nullable WebSocketHttpHeaders handshakeHeaders;

	public WebSocketStompSessionManager(WebSocketStompClient webSocketStompClient, String url, Object... uriVariables) {
		super(webSocketStompClient);
		Assert.hasText(url, "'url' must not be empty.");
		this.url = url;
		this.uriVariables = uriVariables != null ?
				Arrays.copyOf(uriVariables, uriVariables.length) : new Object[0];
	}

	public void setHandshakeHeaders(WebSocketHttpHeaders handshakeHeaders) {
		this.handshakeHeaders = handshakeHeaders;
	}

	@Override
	protected CompletableFuture<StompSession> doConnect(StompSessionHandler handler) {
		return ((WebSocketStompClient) this.stompClient)
				.connectAsync(this.url, this.handshakeHeaders, getConnectHeaders(), handler, this.uriVariables);
	}

}
