/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp;

import java.util.concurrent.CompletableFuture;

import org.springframework.messaging.simp.stomp.ReactorNettyTcpStompClient;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;

/**
 * The {@link ReactorNettyTcpStompClient} based {@link AbstractStompSessionManager} implementation.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see ReactorNettyTcpStompClient
 */
public class ReactorNettyTcpStompSessionManager extends AbstractStompSessionManager {

	public ReactorNettyTcpStompSessionManager(ReactorNettyTcpStompClient reactorNettyTcpStompClient) {
		super(reactorNettyTcpStompClient);
	}

	@Override
	protected CompletableFuture<StompSession> doConnect(StompSessionHandler handler) {
		return ((ReactorNettyTcpStompClient) this.stompClient).connectAsync(getConnectHeaders(), handler);
	}

}
