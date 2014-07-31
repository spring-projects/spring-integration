/*
 * Copyright 2014 the original author or authors.
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

import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.PingMessage;
import org.springframework.web.socket.PongMessage;
import org.springframework.web.socket.WebSocketMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.client.jetty.JettyWebSocketClient;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class ClientWebSocketContainerTests {

	private final static JettyWebSocketTestServer server = new JettyWebSocketTestServer(TestServerConfig.class);

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
		ClientWebSocketContainer container =
				new ClientWebSocketContainer(new JettyWebSocketClient(), server.getWsBaseUrl() + "/ws/websocket");

		TestWebSocketListener messageListener = new TestWebSocketListener();
		container.setMessageListener(messageListener);

		container.start();

		WebSocketSession session = container.getSession(null);
		assertNotNull(session);
		assertTrue(session.isOpen());
		assertEquals("v10.stomp", session.getAcceptedProtocol());

		//TODO Jetty Server treats empty ByteBuffer as 'null' for PongMessage
		session.sendMessage(new PingMessage(ByteBuffer.wrap("ping".getBytes())));

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
