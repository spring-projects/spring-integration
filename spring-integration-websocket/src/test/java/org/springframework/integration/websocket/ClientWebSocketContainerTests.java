/*
 * Copyright 2014-present the original author or authors.
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

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import jakarta.websocket.DeploymentException;
import org.apache.tomcat.websocket.Constants;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpHeaders;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketExtension;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

/**
 * @author Artem Bilan
 * @author Julian Koch
 * @author Glenn Renfro
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
			protected CompletableFuture<WebSocketSession> executeInternal(WebSocketHandler webSocketHandler,
					HttpHeaders headers, URI uri, List<String> protocols, List<WebSocketExtension> extensions,
					Map<String, Object> attributes) {

				CompletableFuture<WebSocketSession> future =
						super.executeInternal(webSocketHandler, headers, uri, protocols, extensions,
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
				new ClientWebSocketContainer(webSocketClient, new URI(server.getWsBaseUrl() + "/ws/websocket"));

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

		assertThatIllegalStateException()
				.isThrownBy(container::start)
				.withCauseInstanceOf(CancellationException.class);

		failure.set(false);

		container.start();

		session = container.getSession(null);
		assertThat(session).isNotNull();
		assertThat(session.isOpen()).isTrue();
	}

	@Test
	public void testWebSocketContainerOverflowStrategyPropagation() throws Exception {
		StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

		Map<String, Object> userProperties = new HashMap<>();
		userProperties.put(Constants.IO_TIMEOUT_MS_PROPERTY, "" + (Constants.IO_TIMEOUT_MS_DEFAULT * 6));
		webSocketClient.setUserProperties(userProperties);

		ClientWebSocketContainer container =
				new ClientWebSocketContainer(webSocketClient, new URI(server.getWsBaseUrl() + "/ws/websocket"));

		container.setSendTimeLimit(10_000);
		container.setSendBufferSizeLimit(12345);
		container.setSendBufferOverflowStrategy(ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);

		TestWebSocketListener messageListener = new TestWebSocketListener();
		container.setMessageListener(messageListener);
		container.setConnectionTimeout(30);

		container.start();

		assertThat(messageListener.sessionStartedLatch.await(10, TimeUnit.SECONDS)).isTrue();

		assertThat(messageListener.sendTimeLimit).isEqualTo(10_000);
		assertThat(messageListener.sendBufferSizeLimit).isEqualTo(12345);
		assertThat(messageListener.sendBufferOverflowStrategy)
				.isEqualTo(ConcurrentWebSocketSessionDecorator.OverflowStrategy.DROP);
	}

	@Test
	public void webSocketContainerFailsOnStartForInvalidUrl() {
		StandardWebSocketClient webSocketClient = new StandardWebSocketClient();

		ClientWebSocketContainer container =
				new ClientWebSocketContainer(webSocketClient, server.getWsBaseUrl() + "/no_such_endpoint");

		assertThatIllegalStateException()
				.isThrownBy(container::start)
				.withCauseExactlyInstanceOf(DeploymentException.class)
				.withStackTraceContaining(
						"The HTTP response from the server [404] did not permit the HTTP upgrade to WebSocket");
	}

	private static class TestWebSocketListener implements WebSocketListener {

		public final CountDownLatch messageLatch = new CountDownLatch(1);

		public final CountDownLatch sessionStartedLatch = new CountDownLatch(1);

		public final CountDownLatch sessionEndedLatch = new CountDownLatch(1);

		public WebSocketMessage<?> message;

		public boolean started;

		int sendTimeLimit;

		int sendBufferSizeLimit;

		ConcurrentWebSocketSessionDecorator.OverflowStrategy sendBufferOverflowStrategy;

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

			var sessionDecorator = (ConcurrentWebSocketSessionDecorator) session;
			this.sendTimeLimit = sessionDecorator.getSendTimeLimit();
			this.sendBufferSizeLimit = sessionDecorator.getBufferSizeLimit();
			this.sendBufferOverflowStrategy =
					TestUtils.getPropertyValue(sessionDecorator, "overflowStrategy");

			this.sessionStartedLatch.countDown();
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
