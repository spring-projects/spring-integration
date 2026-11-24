/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
