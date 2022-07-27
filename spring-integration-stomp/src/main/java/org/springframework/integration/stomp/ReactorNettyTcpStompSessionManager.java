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
