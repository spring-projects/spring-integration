/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.websocket;


import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tomcat.websocket.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
public class ClientWebSocketContainerTests {

	private static final TomcatWebSocketTestServer server = new TomcatWebSocketTestServer(TestServerConfig.class);

	@BeforeAll
	public static void setup() throws Exception {
		server.afterPropertiesSet();
	}

	@AfterAll
	public static void tearDown() throws Exception {
		server.destroy();
	}

	@Test
	public void testClientWebSocketContainer() throws Exception {
		AtomicBoolean failure = new AtomicBoolean();

		StandardWebSocketClient webSocketClient = new StandardWebSocketClient() {

			@Override
			protected ListenableFuture<WebSocketSession> doHandshakeInternal(WebSocketHandler webSocketHandler,
					HttpHeaders headers, URI uri, List<String> protocols, List<WebSocketExtension> extensions,
					Map<String, Object> attributes) {

				ListenableFuture<WebSocketSession> future =
						super.doHandshakeInternal(webSocketHandler, headers, uri, protocols, extensions,
								attributes);
				if (failure.get()) {
					future.cancel(true);
				}

				return future;
			}

		};

		Map<String, Object> userProperties = new HashMap<>();
		userProperties.put(Constants.IO_TIMEOUT_MS_PROPERTY, "" + (Constants.IO_TIMEOUT_MS_DEFAULT * 6));
		webSocketClient.setUserProperties(userProperties);

		ClientWebSocketContainer container =
				new ClientWebSocketContainer(webSocketClient, server.getWsBaseUrl() + "/ws/websocket");

		TestWebSocketListener messageListener = new TestWebSocketListener();
		container.setMessageListener(messageListener);
		container.setConnectionTimeout(30);

		container.start();

		WebSocketSession session = container.getSession(null);
		assertThat(session).isNotNull();
		assertThat(session.isOpen()).isTrue();
		assertThat(session.getAcceptedProtocol()).isEqualTo("v10.stomp");

		session.sendMessage(new PingMessage());

		assertThat(messageListener.messageLatch.await(10, TimeUnit.SECONDS)).isTrue();

		container.stop();

		assertThatIllegalStateException()
				.isThrownBy(() -> container.getSession(null))
				.withMessage("'clientSession' has not been established. Consider to 'start' this container.");

		assertThat(messageListener.sessionEndedLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(session.isOpen()).isFalse();
		assertThat(messageListener.started).isTrue();
		assertThat(messageListener.message).isInstanceOf(PongMessage.class);

		failure.set(true);

		container.start();

		assertThatIllegalStateException()
				.isThrownBy(() -> container.getSession(null))
				.withCauseInstanceOf(CancellationException.class);

		failure.set(false);

		container.start();

		session = container.getSession(null);
		assertThat(session).isNotNull();
		assertThat(session.isOpen()).isTrue();
	}

	private static class TestWebSocketListener implements WebSocketListener {

		public final CountDownLatch messageLatch = new CountDownLatch(1);

		public final CountDownLatch sessionEndedLatch = new CountDownLatch(1);

		public WebSocketMessage<?> message;

		public boolean started;

		TestWebSocketListener() {
		}

		@Override
		public void onMessage(WebSocketSession session, WebSocketMessage<?> message) {
			this.message = message;
			this.messageLatch.countDown();
		}

		@Override
		public void afterSessionStarted(WebSocketSession session) {
			this.started = true;
		}

		@Override
		public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus) {
			sessionEndedLatch.countDown();
		}

		@Override
		public List<String> getSubProtocols() {
			return Collections.singletonList("v10.stomp");
		}

	}

}
