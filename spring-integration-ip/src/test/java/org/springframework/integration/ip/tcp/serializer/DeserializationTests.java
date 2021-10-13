/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.tcp.TcpInboundGateway;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;
import org.springframework.integration.ip.tcp.connection.TcpNioClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.ip.util.TestingUtilities;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.support.LongRunningIntegrationTest;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Gary Russell
 * @author Gavin Gray
 *
 * @since 2.0
 */
public class DeserializationTests {

	@Rule
	public LongRunningIntegrationTest longRunningIntegrationTest = new LongRunningIntegrationTest();

	@Test
	public void testReadLength() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendLength(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		server.close();
		done.countDown();
	}

	@Test
	public void testReadStxEtx() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtx(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		server.close();
		done.countDown();
	}

	@Test
	public void testReadCrLf() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendCrLf(port, null);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		server.close();
		done.countDown();
	}

	@Test
	public void testReadRaw() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		SocketTestUtils.testSendRaw(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		server.close();
	}

	@Test
	public void testReadRawElastic() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		SocketTestUtils.testSendRaw(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayElasticRawDeserializer serializer = new ByteArrayElasticRawDeserializer();
		byte[] out = serializer.deserialize(socket.getInputStream());
		assertThat(new String(out)).as("Data").isEqualTo(SocketTestUtils.TEST_STRING + SocketTestUtils.TEST_STRING);
		try {
			serializer.deserialize(socket.getInputStream());
			fail("Expected end of Stream");
		}
		catch (SoftEndOfStreamException e) {
			// NOSONAR
		}
		server.close();
	}

	@Test
	public void testReadSerialized() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendSerialized(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		DefaultDeserializer deserializer = new DefaultDeserializer();
		Object out = deserializer.deserialize(socket.getInputStream());
		assertThat(out).as("Data").isEqualTo(SocketTestUtils.TEST_STRING);
		out = deserializer.deserialize(socket.getInputStream());
		assertThat(out).as("Data").isEqualTo(SocketTestUtils.TEST_STRING);
		server.close();
		done.countDown();
	}

	@Test
	public void testReadLengthOverflow() throws Exception {
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendLengthOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
			fail("Expected message length exceeded exception");
		}
		catch (IOException e) {
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
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
			fail("Expected timeout exception");
		}
		catch (IOException e) {
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
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch done = SocketTestUtils.testSendStxEtxOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(5000);
		ByteArrayStxEtxSerializer serializer = new ByteArrayStxEtxSerializer();
		serializer.setMaxMessageSize(1024);
		try {
			serializer.deserialize(socket.getInputStream());
			fail("Expected message length exceeded exception");
		}
		catch (IOException e) {
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
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
		server.setSoTimeout(10000);
		CountDownLatch latch = SocketTestUtils.testSendCrLfOverflow(port);
		Socket socket = server.accept();
		socket.setSoTimeout(500);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		try {
			serializer.deserialize(socket.getInputStream());
			fail("Expected timout exception");
		}
		catch (IOException e) {
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
		ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(0);
		int port = server.getLocalPort();
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
			assertThat(bytes.length).isEqualTo(1);
			assertThat(bytes[0]).isEqualTo("s".getBytes()[0]);
			bytes = serializer.deserialize(inputStream);
			assertThat(bytes.length).isEqualTo(0);
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
		assertThat(event.getOffset()).isEqualTo(-1);
		assertThat(new String(event.getBuffer()).substring(0, 3)).isEqualTo(new String(new byte[] { 0, 0, 0 }));
		event = doDeserialize(new ByteArrayLengthHeaderSerializer(),
				"Stream closed after 1 of 2", new byte[] { 0, 0, 0, 2, 7 }, 5); // closed during data read
		assertThat(event.getOffset()).isEqualTo(-1);
		assertThat(new String(event.getBuffer()).substring(0, 1)).isEqualTo(new String(new byte[] { 7 }));
		doDeserialize(new ByteArrayLfSerializer(), "Terminator '0xa' not found before max message length: 5");
		doDeserialize(new ByteArrayRawSerializer(), "Socket was not closed before max message length: 5");
		doDeserialize(new ByteArraySingleTerminatorSerializer((byte) 0xfe), "Terminator '0xfe' not found before max message length: 5");
		doDeserialize(new ByteArrayStxEtxSerializer(), "Expected STX to begin message");
		event = doDeserialize(new ByteArrayStxEtxSerializer(),
				"Socket closed during message assembly", new byte[] { 0x02, 0, 0 }, 5);
		assertThat(event.getOffset()).isEqualTo(2);
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

			@Override
			public void publishEvent(Object event) {

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
			assertThat(event.get()).isNotNull();
			assertThat(event.get().getCause()).isSameAs(e);
			assertThat(e.getMessage()).contains(expectedMessage);
		}
		return event.get();
	}

	@Test
	public void testTimeoutWithCustomDeserializer() throws Exception {
		testTimeoutWhileDecoding(new CustomDeserializer(), "\u0000\u0002\u0000\u0005reply");
	}

	@Test
	public void testTimeoutWithRawDeserializer() throws Exception {
		testTimeoutWhileDecoding(new ByteArrayRawSerializer(), "reply");
	}

	public void testTimeoutWhileDecoding(AbstractByteArraySerializer deserializer, String reply) throws Exception {
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		TcpNioServerConnectionFactory serverNio = new TcpNioServerConnectionFactory(0);
		ByteArrayLengthHeaderSerializer lengthHeaderSerializer = new ByteArrayLengthHeaderSerializer(1);
		serverNio.setDeserializer(lengthHeaderSerializer);
		serverNio.setSerializer(serializer);
		serverNio.afterPropertiesSet();
		TcpInboundGateway in = new TcpInboundGateway();
		in.setConnectionFactory(serverNio);
		QueueChannel serverSideChannel = new QueueChannel();
		in.setRequestChannel(serverSideChannel);
		in.setBeanFactory(mock(BeanFactory.class));
		in.afterPropertiesSet();
		in.start();
		TestingUtilities.waitListening(serverNio, null);
		TcpNioClientConnectionFactory clientNio = new TcpNioClientConnectionFactory("localhost", serverNio.getPort());
		clientNio.setSerializer(serializer);
		clientNio.setDeserializer(deserializer);
		clientNio.setSoTimeout(1000);
		clientNio.afterPropertiesSet();
		final TcpOutboundGateway out = new TcpOutboundGateway();
		out.setConnectionFactory(clientNio);
		QueueChannel outputChannel = new QueueChannel();
		out.setOutputChannel(outputChannel);
		out.setRemoteTimeout(60000);
		out.setBeanFactory(mock(BeanFactory.class));
		out.afterPropertiesSet();
		out.start();
		Runnable command = () -> {
			try {
				out.handleMessage(MessageBuilder.withPayload("\u0004Test").build());
			}
			catch (Exception e) {
				// eat SocketTimeoutException. Doesn't matter for this test
			}
		};
		Executor exec = new SimpleAsyncTaskExecutor("-");

		Message<?> message;

		// short reply should not be received.
		exec.execute(command);
		message = serverSideChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test");
		String shortReply = reply.substring(0, reply.length() - 1);
		((MessageChannel) message.getHeaders().getReplyChannel()).send(new GenericMessage<String>(shortReply));
		message = outputChannel.receive(1000);
		assertThat(message).isNull();
	}

	@Test
	public void testTimeoutWithRawDeserializerEofIsTerminator() throws Exception {
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		TcpNioServerConnectionFactory serverNio = new TcpNioServerConnectionFactory(0);
		ByteArrayLengthHeaderSerializer lengthHeaderSerializer = new ByteArrayLengthHeaderSerializer(1);
		serverNio.setDeserializer(lengthHeaderSerializer);
		serverNio.setSerializer(serializer);
		serverNio.afterPropertiesSet();
		TcpInboundGateway in = new TcpInboundGateway();
		in.setConnectionFactory(serverNio);
		QueueChannel serverSideChannel = new QueueChannel();
		in.setRequestChannel(serverSideChannel);
		in.setBeanFactory(mock(BeanFactory.class));
		in.afterPropertiesSet();
		in.start();
		TestingUtilities.waitListening(serverNio, null);
		TcpNioClientConnectionFactory clientNio = new TcpNioClientConnectionFactory("localhost", serverNio.getPort());
		clientNio.setSerializer(serializer);
		clientNio.setDeserializer(new ByteArrayRawSerializer(true));
		clientNio.setSoTimeout(1000);
		clientNio.afterPropertiesSet();
		final TcpOutboundGateway out = new TcpOutboundGateway();
		out.setConnectionFactory(clientNio);
		QueueChannel outputChannel = new QueueChannel();
		out.setOutputChannel(outputChannel);
		out.setRemoteTimeout(60000);
		out.setBeanFactory(mock(BeanFactory.class));
		out.afterPropertiesSet();
		out.start();
		Runnable command = () -> {
			try {
				out.handleMessage(MessageBuilder.withPayload("\u0004Test").build());
			}
			catch (Exception e) {
				// eat SocketTimeoutException. Doesn't matter for this test
			}
		};
		Executor exec = new SimpleAsyncTaskExecutor("testTimeoutWithRawDeserializerEofIsTerminator-");

		Message<?> message;

		exec.execute(command);
		message = serverSideChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String((byte[]) message.getPayload())).isEqualTo("Test");
		((MessageChannel) message.getHeaders().getReplyChannel()).send(new GenericMessage<String>("reply"));
		message = outputChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(new String(((byte[]) message.getPayload()))).isEqualTo("reply");
	}

	private static class CustomDeserializer extends AbstractByteArraySerializer {

		@Override
		public byte[] deserialize(InputStream inputStream) throws IOException {
			if (logger.isDebugEnabled()) {
				logger.debug("Available to read:" + inputStream.available());
			}

			byte[] header = new byte[2];
			header[0] = (byte) inputStream.read();
			if (header[0] < 0) {
				throw new SoftEndOfStreamException("Stream closed between payloads");
			}

			header[1] = (byte) inputStream.read();
			if (header[1] < 0) {
				checkClosure(-1);
			}

			ByteBuffer headerBB = ByteBuffer.wrap(header);
			int val = headerBB.getShort();

			byte[] length = new byte[val];
			for (int i = 0; i < val; i++) {
				length[i] = (byte) inputStream.read();
			}

			ByteBuffer lengthBB = ByteBuffer.wrap(length);
			int messageLength;
			if (val == 2) {
				messageLength = lengthBB.getShort();
			}
			else if (val == 4) {
				messageLength = lengthBB.getInt();
			}
			else {
				throw new IOException("Unexpected count of bytes that holds message length");
			}

			byte[] answer = new byte[messageLength];
			for (int i = 0; i < messageLength; i++) {
				int bite = inputStream.read();
				if (bite < 0) {
					checkClosure(-1);
				}
				answer[i] = (byte) bite;
			}

			ByteBuffer b = ByteBuffer.allocate(2 + val + messageLength);
			b.put(header);
			b.put(length);
			b.put(answer);
			return b.array();
		}

		@Override
		public void serialize(byte[] object, OutputStream outputStream) throws IOException {
		}

	}

}
