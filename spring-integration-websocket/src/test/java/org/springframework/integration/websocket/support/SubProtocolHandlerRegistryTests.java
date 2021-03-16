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

package org.springframework.integration.websocket.support;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.messaging.StompSubProtocolHandler;
import org.springframework.web.socket.messaging.SubProtocolHandler;

/**
 * @author Artem Bilan
 *
 * @since 4.1
 */
public class SubProtocolHandlerRegistryTests {

	@Test
	public void testProtocolHandlers() {
		SubProtocolHandler defaultProtocolHandler = mock(SubProtocolHandler.class);
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				new SubProtocolHandlerRegistry(
						Collections.singletonList(new StompSubProtocolHandler()),
						defaultProtocolHandler);
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("v10.stomp", (String) null);
		SubProtocolHandler protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(protocolHandler).isInstanceOf(StompSubProtocolHandler.class);
		protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(defaultProtocolHandler).isSameAs(protocolHandler);

		assertThat(new StompSubProtocolHandler().getSupportedProtocols())
				.isEqualTo(subProtocolHandlerRegistry.getSubProtocols());
	}

	@Test
	public void testSingleHandler() {
		SubProtocolHandler testProtocolHandler = spy(new StompSubProtocolHandler());
		when(testProtocolHandler.getSupportedProtocols()).thenReturn(Collections.singletonList("foo"));
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				new SubProtocolHandlerRegistry(Collections.singletonList(testProtocolHandler));
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("foo", (String) null);
		SubProtocolHandler protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(testProtocolHandler).isSameAs(protocolHandler);

		protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(testProtocolHandler).isSameAs(protocolHandler);
	}

	@Test
	public void testHandlerSelection() {
		SubProtocolHandler testProtocolHandler = new StompSubProtocolHandler();
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				new SubProtocolHandlerRegistry(testProtocolHandler);
		WebSocketSession session = mock(WebSocketSession.class);
		when(session.getAcceptedProtocol()).thenReturn("foo", "", null);

		assertThatIllegalStateException()
				.isThrownBy(() -> subProtocolHandlerRegistry.findProtocolHandler(session))
				.withMessageContaining("No handler for sub-protocol 'foo'");

		SubProtocolHandler protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(testProtocolHandler).isSameAs(protocolHandler);

		protocolHandler = subProtocolHandlerRegistry.findProtocolHandler(session);
		assertThat(protocolHandler).isNotNull();
		assertThat(testProtocolHandler).isSameAs(protocolHandler);
	}

	@Test
	public void testResolveSessionId() {
		SubProtocolHandlerRegistry subProtocolHandlerRegistry =
				new SubProtocolHandlerRegistry(new StompSubProtocolHandler());

		Message<String> message = MessageBuilder.withPayload("foo")
				.setHeader(SimpMessageHeaderAccessor.SESSION_ID_HEADER, "TEST_SESSION")
				.build();

		String sessionId = subProtocolHandlerRegistry.resolveSessionId(message);
		assertThat("TEST_SESSION").isEqualTo(sessionId);

		message = MessageBuilder.withPayload("foo")
				.setHeader("MY_SESSION_ID", "TEST_SESSION")
				.build();

		sessionId = subProtocolHandlerRegistry.resolveSessionId(message);
		assertThat(sessionId).isNull();
	}

}
