/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.junit.Test;

import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.test.util.SocketUtils;

/**
 * @author Gary Russell
 * @author Gavin Gray
 * @since 2.0
 */
public class DeserializationTests {

	@Test
	public void testReadLength() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendLength(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
		done.countDown();
	}

	@Test
	public void testReadStxEtx() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtx(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
		done.countDown();
	}

	@Test
	public void testReadCrLf() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendCrLf(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
				 new String(out));
		server.close();
		done.countDown();
	}

	@Test
	public void testReadRaw() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		SocketTestUtils.testSendRaw(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING,
								 new String(out));
		server.close();
	}

	@Test
	public void testReadSerialized() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendSerialized(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		DefaultDeserializer deserializer = new DefaultDeserializer();
		Object out = deserializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING, out);
		out = deserializer.deserialize(socket.getInputStream());
		assertEquals("Data", SocketTestUtils.TEST_STRING, out);
		server.close();
		done.countDown();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendLengthOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Message length")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
		done.countDown();
	}

	@Test
	public void testReadStxEtxTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected timeout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
		done.countDown();
	}

	@Test
	public void testReadStxEtxOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		serializer.setMaxMessageSize(1024);
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("ETX not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
		done.countDown();
	}

	@Test
	public void testReadCrLfTimeout() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch latch = SocketTestUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected timout exception");
		} catch (IOException e) {
			if (!e.getMessage().startsWith("Read timed out")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
		latch.countDown();
	}

	@Test
	public void testReadCrLfOverflow() throws Exception {
		int port = SocketUtils.findAvailableServerSocket();
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
		server.setSoTimeout(10000);
		CountDownLatch latch = SocketTestUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		serializer.setMaxMessageSize(1024);
		try {
			serializer.deserialize(socket.getInputStream());
	    	fail("Expected message length exceeded exception");
		}
		catch (IOException e) {
			if (!e.getMessage().startsWith("CRLF not found")) {
				e.printStackTrace();
				fail("Unexpected IO Error:" + e.getMessage());
			}
		}
		server.close();
		latch.countDown();
	}

	@Test
	public void canDeserializeMultipleSubsequentTerminators() throws IOException {
		byte terminator = (byte) '\n';
		ByteArraySingleTerminatorSerializer serializer = new ByteArraySingleTerminatorSerializer(terminator);
		ByteArrayInputStream inputStream = new ByteArrayInputStream("s\n\n".getBytes());

		try {
			byte[] bytes = serializer.deserialize(inputStream);
			assertEquals(1, bytes.length);
			assertEquals("s".getBytes()[0], bytes[0]);
			bytes = serializer.deserialize(inputStream);
			assertEquals(0, bytes.length);
		}
		finally {
			inputStream.close();
		}
	}

	@Test
	public void deserializationEvents() throws Exception {
		doDeserialize(new ByteArrayCrLfSerializer(), "CRLF not found before max message length: 5");
		doDeserialize(new ByteArrayLengthHeaderSerializer(), "Message length 1718579042 exceeds max message length: 5");
		TcpDeserializationExceptionEvent event = doDeserialize(new ByteArrayLengthHeaderSerializer(),
				"Stream closed after 3 of 4", new byte[] { 0, 0, 0 }, 5); // closed during header read
		assertEquals(-1, event.getOffset());
		assertEquals(new String(new byte[] { 0, 0, 0 }), new String(event.getBuffer()).substring(0, 3));
		event = doDeserialize(new ByteArrayLengthHeaderSerializer(),
				"Stream closed after 1 of 2", new byte[] { 0, 0, 0, 2, 7 }, 5); // closed during data read
		assertEquals(-1, event.getOffset());
		assertEquals(new String(new byte[] { 7 }), new String(event.getBuffer()).substring(0, 1));
		doDeserialize(new ByteArrayLfSerializer(), "Terminator '0xa' not found before max message length: 5");
		doDeserialize(new ByteArrayRawSerializer(), "Socket was not closed before max message length: 5");
		doDeserialize(new ByteArraySingleTerminatorSerializer((byte) 0xfe), "Terminator '0xfe' not found before max message length: 5");
		doDeserialize(new ByteArrayStxEtxSerializer(), "Expected STX to begin message");
		event = doDeserialize(new ByteArrayStxEtxSerializer(),
				"Socket closed during message assembly", new byte[] { 0x02, 0, 0 }, 5);
		assertEquals(2, event.getOffset());
	}

	private TcpDeserializationExceptionEvent doDeserialize(AbstractByteArraySerializer deser, String expectedMessage) {
		return doDeserialize(deser, expectedMessage, "foobar".getBytes(), 5);
	}

	private TcpDeserializationExceptionEvent doDeserialize(AbstractByteArraySerializer deser, String expectedMessage,
			byte[] data, int mms) {
		final AtomicReference<TcpDeserializationExceptionEvent> event =
				new AtomicReference<TcpDeserializationExceptionEvent>();
		class Publisher implements ApplicationEventPublisher {

			@Override
			public void publishEvent(ApplicationEvent anEvent) {
				event.set((TcpDeserializationExceptionEvent) anEvent);
			}
		}
		Publisher publisher = new Publisher();
		ByteArrayInputStream bais = new ByteArrayInputStream(data);
		deser.setApplicationEventPublisher(publisher);
		deser.setMaxMessageSize(mms);
		try {
			deser.deserialize(bais);
			fail("expected exception");
		}
		catch (Exception e) {
			assertNotNull(event.get());
			assertSame(e, event.get().getCause());
			assertThat(e.getMessage(), containsString(expectedMessage));
		}
		return event.get();
	}

}
