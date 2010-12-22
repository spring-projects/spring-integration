/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ip.tcp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import javax.net.SocketFactory;

import org.junit.Test;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.SubscribableChannel;
import org.springframework.integration.handler.ServiceActivatingHandler;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.HelloWorldInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnectionInterceptorFactoryChain;
import org.springframework.integration.ip.tcp.connection.TcpNetServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNioServerConnectionFactory;
import org.springframework.integration.ip.tcp.serializer.ByteArrayCrLfSerializer;
import org.springframework.integration.ip.util.SocketTestUtils;

/**
 * @author Gary Russell
 */
public class TcpReceivingChannelAdapterTests {

	@Test
	public void newTestNet() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test1", new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		assertEquals("Test2", new String((byte[]) message.getPayload()));
	}

	@Test
	public void newTestNio() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);		
		scf.setSoTimeout(5000);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		for (int i = 0; i < 1000; i++) {
			socket.getOutputStream().write(("Test" + i + "\r\n").getBytes());
		}	
		Set<String> results = new HashSet<String>();
		for (int i = 0; i < 1000; i++) {
			Message<?> message = channel.receive(10000);
			assertNotNull(message);
			results.add(new String((byte[]) message.getPayload()));
		}
		for (int i = 0; i < 1000; i++) {
			assertTrue(results.remove("Test" + i));
		}
	}

	@Test
	public void newTestNetShared() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);		
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
	}
	
	@Test
	public void newTestNioShared() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(2000);
		socket.getOutputStream().write("Test\r\n".getBytes());
		socket.getOutputStream().write("Test\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);		
		byte[] b = new byte[6];
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
		readFully(socket.getInputStream(), b);
		assertEquals("Test\r\n", new String(b));
	}
	
	@Test
	public void newTestNetSingleNoOutbound() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(new String((byte[]) message.getPayload()));
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	@Test
	public void newTestNioSingleNoOutbound() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<String> results = new HashSet<String>();
		results.add(new String((byte[]) message.getPayload()));
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(new String((byte[]) message.getPayload()));
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	/**
	 * @param is
	 * @param buff
	 */
	private void readFully(InputStream is, byte[] buff) throws IOException {
		for (int i = 0; i < buff.length; i++) {
			buff[i] = (byte) is.read();
		}
	}

	@Test
	public void newTestNetSingleShared() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);		
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertEquals("Test1\r\n", new String(b));
		readFully(socket2.getInputStream(), b);
		assertEquals("Test2\r\n", new String(b));
	}
	
	@Test  
	public void newTestNioSingleShared() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(2000);
		socket1.getOutputStream().write("Test1\r\n".getBytes());
		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(2000);
		socket2.getOutputStream().write("Test2\r\n".getBytes());
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);		
		byte[] b = new byte[7];
		readFully(socket1.getInputStream(), b);
		assertEquals("Test1\r\n", new String(b));
		readFully(socket2.getInputStream(), b);
		assertEquals("Test2\r\n", new String(b));
	}
	
	@Test  
	public void newTestNioSingleSharedMany() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		TcpNioServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		scf.setSingleUse(true);
		scf.setPoolSize(100);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		Executor te = Executors.newFixedThreadPool(10);
		scf.setTaskExecutor(te);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		List<Socket> sockets = new LinkedList<Socket>();
		for (int i = 100; i < 200; i++) {
			Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
			socket1.setSoTimeout(2000);
			socket1.getOutputStream().write(("Test" + i + "\r\n").getBytes());
			sockets.add(socket1);
		}
		for (int i = 100; i < 200; i++) {
			Message<?> message = channel.receive(10000);
			assertNotNull(message);
			handler.handleMessage(message);
		}
		byte[] b = new byte[9];
		for (int i = 100; i < 200; i++) { 
			readFully(sockets.remove(0).getInputStream(), b);
			assertEquals("Test" + i + "\r\n", new String(b));
		}
	}
	
	@Test
	public void newTestNetInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		interceptorsGuts(port, scf);
	}

	@Test
	public void newTestNetSingleNoOutboundInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		singleNoOutboundInterceptorsGuts(port, scf);
	}

	@Test
	public void newTestNetSingleSharedInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		singleSharedInterceptorsGuts(port, scf);
	}
	
	@Test
	public void newTestNioInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		interceptorsGuts(port, scf);
	}

	@Test
	public void newTestNioSingleNoOutboundInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		singleNoOutboundInterceptorsGuts(port, scf);
	}

	@Test
	public void newTestNioSingleSharedInterceptors() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNioServerConnectionFactory(port);
		singleSharedInterceptorsGuts(port, scf);
	}

	private void interceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(false);		
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] 
            		     {new HelloWorldInterceptorFactory(),
               		      new HelloWorldInterceptorFactory()});
		scf.setInterceptorFactoryChain(fc);
		scf.setSoTimeout(10000);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Set<String> results = new HashSet<String>();
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		results.add((String) message.getPayload());
		message = channel.receive(10000);
		assertNotNull(message);
		results.add((String) message.getPayload());
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	private void singleNoOutboundInterceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(true);
		scf.setSoTimeout(10000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] 
            		     {new HelloWorldInterceptorFactory(),
               		      new HelloWorldInterceptorFactory()});
		scf.setInterceptorFactoryChain(fc);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.setSoTimeout(10000);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test1");
		
		socket = SocketFactory.getDefault().createSocket("localhost", port);
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket.getInputStream()).readObject());
		new ObjectOutputStream(socket.getOutputStream()).writeObject("Test2");
		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		// with single use, results may come back in a different order
		Set<Object> results = new HashSet<Object>();
		results.add(message.getPayload());
		message = channel.receive(10000);
		assertNotNull(message);
		results.add(message.getPayload());
		assertTrue(results.contains("Test1"));
		assertTrue(results.contains("Test2"));
	}

	private void singleSharedInterceptorsGuts(final int port, AbstractServerConnectionFactory scf) throws Exception {
		scf.setSerializer(new DefaultSerializer());
		scf.setDeserializer(new DefaultDeserializer());
		scf.setSingleUse(true);
		scf.setSoTimeout(60000);
		TcpConnectionInterceptorFactoryChain fc = new TcpConnectionInterceptorFactoryChain();
		fc.setInterceptors(new TcpConnectionInterceptorFactory[] 
            		     {new HelloWorldInterceptorFactory(),
               		      new HelloWorldInterceptorFactory()});
		scf.setInterceptorFactoryChain(fc);
		TcpSendingMessageHandler handler = new TcpSendingMessageHandler();
		handler.setConnectionFactory(scf);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		QueueChannel channel = new QueueChannel();
		adapter.setOutputChannel(channel);
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to listen");
			}
		}
		Socket socket1 = SocketFactory.getDefault().createSocket("localhost", port);
		socket1.setSoTimeout(60000);
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket1.getInputStream()).readObject());
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket1.getInputStream()).readObject());
		new ObjectOutputStream(socket1.getOutputStream()).writeObject("Test1");

		Socket socket2 = SocketFactory.getDefault().createSocket("localhost", port);
		socket2.setSoTimeout(60000);
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket2.getInputStream()).readObject());
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Hello");
		assertEquals("world!", new ObjectInputStream(socket2.getInputStream()).readObject());
		new ObjectOutputStream(socket2.getOutputStream()).writeObject("Test2");

		Message<?> message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		message = channel.receive(10000);
		assertNotNull(message);
		handler.handleMessage(message);
		
		assertEquals("Test1", new ObjectInputStream(socket1.getInputStream()).readObject());
		assertEquals("Test2", new ObjectInputStream(socket2.getInputStream()).readObject());
	}

	@Test
	public void testException() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractServerConnectionFactory scf = new TcpNetServerConnectionFactory(port);
		ByteArrayCrLfSerializer serializer = new ByteArrayCrLfSerializer();
		scf.setSerializer(serializer);
		scf.setDeserializer(serializer);
		TcpReceivingChannelAdapter adapter = new TcpReceivingChannelAdapter();
		adapter.setConnectionFactory(scf);
		scf.start();
		int n = 0;
		while (!scf.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				fail("Failed to start listening");
			}
		}
		SubscribableChannel channel = new DirectChannel();
		adapter.setOutputChannel(channel);
		ServiceActivatingHandler handler = new ServiceActivatingHandler(new FailingService());
		channel.subscribe(handler);
		Socket socket = SocketFactory.getDefault().createSocket("localhost", port);
		socket.getOutputStream().write("Test1\r\n".getBytes());
		socket.getOutputStream().write("Test2\r\n".getBytes());
		QueueChannel errorChannel = new QueueChannel();
		adapter.setErrorChannel(errorChannel);
		Message<?> message = errorChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Failed", ((Exception) message.getPayload()).getCause().getMessage());
		message = errorChannel.receive(10000);
		assertNotNull(message);
		assertEquals("Failed", ((Exception) message.getPayload()).getCause().getMessage());
	}

	private class FailingService {
		@SuppressWarnings("unused")
		public String serviceMethod(byte[] bytes) {
			throw new RuntimeException("Failed");
		}
	}


}
