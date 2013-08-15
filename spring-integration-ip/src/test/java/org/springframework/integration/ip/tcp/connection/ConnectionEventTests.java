/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.OutputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Serializer;
import org.springframework.integration.message.GenericMessage;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ConnectionEventTests {

	@Test
	public void test() throws Exception {
		Socket socket = mock(Socket.class);
		final AtomicReference<TcpConnectionEvent> theEvent = new AtomicReference<TcpConnectionEvent>();
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, new ApplicationEventPublisher() {

			public void publishEvent(ApplicationEvent event) {
				theEvent.set((TcpConnectionEvent) event);
			}
		}, "foo");
		assertNotNull(theEvent.get());
		assertTrue(theEvent.get() instanceof TcpConnectionOpenEvent);
		assertTrue(theEvent.get().toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "] **OPENED**"));
		@SuppressWarnings("unchecked")
		Serializer<Object> serializer = mock(Serializer.class);
		RuntimeException toBeThrown = new RuntimeException("foo");
		doThrow(toBeThrown).when(serializer).serialize(Mockito.any(Object.class), Mockito.any(OutputStream.class));
		conn.setMapper(new TcpMessageMapper());
		conn.setSerializer(serializer);
		try {
			conn.send(new GenericMessage<String>("bar"));
			fail("Expected exception");
		}
		catch (Exception e) {}
		assertNotNull(theEvent.get());
		assertTrue(theEvent.get() instanceof TcpConnectionExceptionEvent);
		assertTrue(theEvent.get().toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "]"));
		assertTrue(theEvent.get().toString().contains("cause=java.lang.RuntimeException: foo]"));
		TcpConnectionExceptionEvent event = (TcpConnectionExceptionEvent) theEvent.get();
		assertNotNull(event.getCause());
		assertSame(toBeThrown, event.getCause());
		conn.close();
		assertNotNull(theEvent.get());
		assertTrue(theEvent.get().toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "] **CLOSED**"));
	}

}
