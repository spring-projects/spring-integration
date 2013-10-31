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
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.Serializer;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class ConnectionEventTests {

	@Test
	public void test() throws Exception {
		Socket socket = mock(Socket.class);
		final List<TcpConnectionEvent> theEvent = new ArrayList<TcpConnectionEvent>();
		TcpNetConnection conn = new TcpNetConnection(socket, false, false, new ApplicationEventPublisher() {

			public void publishEvent(ApplicationEvent event) {
				theEvent.add((TcpConnectionEvent) event);
			}
		}, "foo");
		/*
		 *  Open is not published by the connection itself; the factory publishes it after initialization.
		 *  See ConnectionToConnectionTests.
		 */
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
		assertTrue(theEvent.size() > 0);
		assertNotNull(theEvent.get(0));
		assertTrue(theEvent.get(0) instanceof TcpConnectionExceptionEvent);
		assertTrue(theEvent.get(0).toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "]"));
		assertTrue(theEvent.get(0).toString().contains("cause=java.lang.RuntimeException: foo]"));
		TcpConnectionExceptionEvent event = (TcpConnectionExceptionEvent) theEvent.get(0);
		assertNotNull(event.getCause());
		assertSame(toBeThrown, event.getCause());
		assertTrue(theEvent.size() > 1);
		assertNotNull(theEvent.get(1));
		assertTrue(theEvent.get(1).toString().endsWith("[factory=foo, connectionId=" + conn.getConnectionId() + "] **CLOSED**"));
	}

}
