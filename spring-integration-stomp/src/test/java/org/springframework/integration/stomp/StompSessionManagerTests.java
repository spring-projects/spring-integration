/*
 * Copyright 2016-2023 the original author or authors.
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
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.simp.stomp.StompClientSupport;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandler;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.scheduling.concurrent.ConcurrentTaskScheduler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 4.2.9
 */
public class StompSessionManagerTests {

	@Test
	public void testDoConnectFailure() throws Exception {
		StompClientSupport stompClient = mock(StompClientSupport.class);
		stompClient.setTaskScheduler(new ConcurrentTaskScheduler(Executors.newSingleThreadScheduledExecutor()));
		AbstractStompSessionManager sessionManager = new AbstractStompSessionManager(stompClient) {

			private final AtomicBoolean thrown = new AtomicBoolean();

			@Override
			protected CompletableFuture<StompSession> doConnect(StompSessionHandler handler) {
				if (!this.thrown.getAndSet(true)) {
					throw new RuntimeException("intentional");
				}
				else {
					CompletableFuture<StompSession> future = new CompletableFuture<>();
					StompSession stompSession = mock(StompSession.class);
					future.complete(stompSession);
					handler.afterConnected(stompSession, getConnectHeaders());
					return future;
				}
			}

		};

		sessionManager.start();

		final CompletableFuture<StompSession> stompSessionFuture = new CompletableFuture<>();
		sessionManager.connect(new StompSessionHandlerAdapter() {

			@Override
			public void afterConnected(StompSession session, StompHeaders connectedHeaders) {
				stompSessionFuture.complete(session);
			}

		});

		assertThat(stompSessionFuture.get(10, TimeUnit.SECONDS)).isNotNull();

		sessionManager.stop();
	}

}
