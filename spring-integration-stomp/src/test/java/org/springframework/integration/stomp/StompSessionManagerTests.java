/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
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
