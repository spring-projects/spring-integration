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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.net.Socket;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.commons.logging.Log;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.integration.ip.tcp.serializer.ByteArrayStxEtxSerializer;

/**
 * @author Gary Russell
 * @since 2.2.2
 *
 */
public class TcpNetConnectionTests {

	@Test
	public void testErrorLog() throws Exception {
		Socket socket = mock(Socket.class);
		InputStream stream = mock(InputStream.class);
		when(socket.getInputStream()).thenReturn(stream);
		when(stream.read()).thenReturn((int) 'x');
		TcpNetConnection connection = new TcpNetConnection(socket, true, false);
		connection.setDeserializer(new ByteArrayStxEtxSerializer());
		final AtomicReference<Object> log = new AtomicReference<Object>();
		Log logger = mock(Log.class);
		doAnswer(new Answer<Object>() {
			public Object answer(InvocationOnMock invocation) throws Throwable {
				log.set(invocation.getArguments()[0]);
				return null;
			}
		}).when(logger).error(Mockito.anyString());
		DirectFieldAccessor accessor = new DirectFieldAccessor(connection);
		accessor.setPropertyValue("logger", logger);
		connection.registerListener(mock(TcpListener.class));
		connection.setMapper(new TcpMessageMapper());
		connection.run();
		assertNotNull(log.get());
		assertEquals("Read exception " +
				connection.getConnectionId() +
				" MessageMappingException:Expected STX to begin message",
				log.get());
	}

}
