/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.websocket;


import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.tomcat.websocket.WsWebSocketContainer;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

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
 * @since 4.1
 */
public class ClientWebSocketContainerTests {

	private final static TomcatWebSocketTestServer server = new TomcatWebSocketTestServer(TestServerConfig.class);

	@BeforeClass
	public static void setup() throws Exception {
		server.afterPropertiesSet();
	}

	@AfterClass
	public static void tearDown() throws Exception {
		server.destroy();
	}

	@Test
	public void testClientWebSocketContainer() throws Exception {

		final AtomicBoolean failure = new AtomicBoolean();

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

		Map<String, Object> userProperties = new HashMap<String, Object>();
		userProperties.put(WsWebSocketContainer.IO_TIMEOUT_MS_PROPERTY,
				"" + (WsWebSocketContainer.IO_TIMEOUT_MS_DEFAULT * 6));
		webSocketClient.setUserProperties(userProperties);

		ClientWebSocketContainer container =
				new ClientWebSocketContainer(webSocketClient, server.getWsBaseUrl() + "/ws/websocket");

		TestWebSocketListener messageListener = new TestWebSocketListener();
		container.setMessageListener(messageListener);
		container.setConnectionTimeout(30);

		container.start();

		WebSocketSession session = container.getSession(null);
		assertNotNull(session);
		assertTrue(session.isOpen());
		assertEquals("v10.stomp", session.getAcceptedProtocol());

		session.sendMessage(new PingMessage());

		assertTrue(messageListener.messageLatch.await(10, TimeUnit.SECONDS));

		container.stop();
		try {
			container.getSession(null);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertEquals(e.getMessage(), "'clientSession' has not been established. Consider to 'start' this container.");
		}

		assertTrue(messageListener.sessionEndedLatch.await(10, TimeUnit.SECONDS));
		assertFalse(session.isOpen());
		assertTrue(messageListener.started);
		assertThat(messageListener.message, instanceOf(PongMessage.class));

		failure.set(true);

		container.start();

		try {
			container.getSession(null);
			fail("IllegalStateException is expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getCause(), instanceOf(CancellationException.class));
		}

		failure.set(false);

		container.start();

		session = container.getSession(null);
		assertNotNull(session);
		assertTrue(session.isOpen());
	}

	private class TestWebSocketListener implements WebSocketListener {

		public boolean started;

		public final CountDownLatch messageLatch = new CountDownLatch(1);

		public WebSocketMessage<?> message;

		public final CountDownLatch sessionEndedLatch = new CountDownLatch(1);

		@Override
		public void onMessage(WebSocketSession session, WebSocketMessage<?> message) throws Exception {
			this.message = message;
			this.messageLatch.countDown();
		}

		@Override
		public void afterSessionStarted(WebSocketSession session) throws Exception {
			this.started = true;
		}

		@Override
		public void afterSessionEnded(WebSocketSession session, CloseStatus closeStatus) throws Exception {
			sessionEndedLatch.countDown();
		}

		@Override
		public List<String> getSubProtocols() {
			return Collections.singletonList("v10.stomp");
		}

	}

}
