/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.stomp;

import org.springframework.messaging.simp.stomp.Reactor2TcpStompClient;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.util.concurrent.ListenableFuture;

/**
 * The {@link Reactor2TcpStompClient} based {@link AbstractStompSessionManager} implementation.
 *
 * @author Artem Bilan
 * @see Reactor2TcpStompClient
 * @since 4.2
 */
public class Reactor2TcpStompSessionManager extends AbstractStompSessionManager {

	public Reactor2TcpStompSessionManager(Reactor2TcpStompClient reactor2TcpStompClient) {
		super(reactor2TcpStompClient);
	}

	@Override
	protected ListenableFuture<StompSession> doConnect(StompSessionHandler handler) {
		return ((Reactor2TcpStompClient) this.stompClient).connect(getConnectHeaders(), handler);
	}

}
