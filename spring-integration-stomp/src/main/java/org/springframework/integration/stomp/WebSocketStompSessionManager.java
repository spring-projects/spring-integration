/*
 * Copyright 2015-2022 the original author or authors.
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

package org.springframework.integration.stomp;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

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

	private volatile WebSocketHttpHeaders handshakeHeaders;

	public WebSocketStompSessionManager(WebSocketStompClient webSocketStompClient, String url, Object... uriVariables) {
		super(webSocketStompClient);
		Assert.hasText(url, "'url' must not be empty.");
		this.url = url;
		this.uriVariables = uriVariables != null ? Arrays.copyOf(uriVariables, uriVariables.length) : null;
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
