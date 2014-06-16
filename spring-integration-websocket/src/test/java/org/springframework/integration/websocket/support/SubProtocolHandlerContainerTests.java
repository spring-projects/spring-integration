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

package org.springframework.integration.websocket.support;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.Collections;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class SubProtocolHandlerContainerTests {

	@Test
	public void testProtocolHandlers() {
		SubProtocolHandler defaultProtocolHandler = mock(SubProtocolHandler.class);
		SubProtocolHandlerContainer subProtocolHandlerContainer =
				new SubProtocolHandlerContainer(
						Collections.<SubProtocolHandler>singletonList(new StompSubProtocolHandler()),
						defaultProtocolHandler);
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("v10.stomp", (String) null);
		SubProtocolHandler protocolHandler = subProtocolHandlerContainer.findProtocolHandler(session);
		assertNotNull(protocolHandler);
		assertThat(protocolHandler, instanceOf(StompSubProtocolHandler.class));
		protocolHandler = subProtocolHandlerContainer.findProtocolHandler(session);
		assertNotNull(protocolHandler);
		assertSame(protocolHandler, defaultProtocolHandler);

		assertEquals(subProtocolHandlerContainer.getSubProtocols(), new StompSubProtocolHandler().getSupportedProtocols());
	}

	@Test
	public void testSingleHandler() {
		SubProtocolHandler testProtocolHandler = spy(new StompSubProtocolHandler());
		when(testProtocolHandler.getSupportedProtocols()).thenReturn(Collections.singletonList("foo"));
		SubProtocolHandlerContainer subProtocolHandlerContainer =
				new SubProtocolHandlerContainer(Collections.<SubProtocolHandler>singletonList(testProtocolHandler));
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("foo", (String) null);
		SubProtocolHandler protocolHandler = subProtocolHandlerContainer.findProtocolHandler(session);
		assertNotNull(protocolHandler);
		assertSame(protocolHandler, testProtocolHandler);

		protocolHandler = subProtocolHandlerContainer.findProtocolHandler(session);
		assertNotNull(protocolHandler);
		assertSame(protocolHandler, testProtocolHandler);
	}

	@Test
	public void testDefaultHandler() {
		SubProtocolHandler testProtocolHandler = new StompSubProtocolHandler();
		SubProtocolHandlerContainer subProtocolHandlerContainer =
				new SubProtocolHandlerContainer(testProtocolHandler);
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("foo", (String) null);

		try {
			subProtocolHandlerContainer.findProtocolHandler(session);
			fail("IllegalStateException expected");
		}
		catch (Exception e) {
			assertThat(e, instanceOf(IllegalStateException.class));
			assertThat(e.getMessage(), containsString("No handler for sub-protocol 'foo', handlers = {}"));
		}

		SubProtocolHandler protocolHandler = subProtocolHandlerContainer.findProtocolHandler(session);
		assertNotNull(protocolHandler);
		assertSame(protocolHandler, testProtocolHandler);
	}

	@Test
	public void testResolveSessionId() {
		SubProtocolHandlerContainer subProtocolHandlerContainer =
				new SubProtocolHandlerContainer(new StompSubProtocolHandler());

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "TEST_SESSION")
				.build();

		String sessionId = subProtocolHandlerContainer.resolveSessionId(message);
		assertEquals(sessionId, "TEST_SESSION");

		message = MessageBuilder.withPayload("foo")
				.setHeader("MY_SESSION_ID", "TEST_SESSION")
				.build();

		sessionId = subProtocolHandlerContainer.resolveSessionId(message);
		assertNull(sessionId);
	}

}
