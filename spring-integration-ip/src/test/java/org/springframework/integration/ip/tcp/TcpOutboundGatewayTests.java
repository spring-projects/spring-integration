/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.net.ServerSocketFactory;

import org.junit.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.ip.util.SocketTestUtils;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGatewayTests {

	@Test 
	public void testGoodNetSingle() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port, 100);
					latch.countDown();
					int i = 0;
					while (true) {
						Socket socket = server.accept();
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						ois.readObject();
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject("Reply" + (i++));
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(true);
		ccf.setPoolSize(10);
		ccf.start();
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		gateway.setReplyTimeout(60000);
		gateway.setRequestTimeout(60000);
		for (int i = 100; i < 200; i++) {
			gateway.handleMessage(MessageBuilder.withPayload("Test" + i).build());
		}
		Set<String> replies = new HashSet<String>();
		for (int i = 100; i < 200; i++) { 
			Message<?> m = replyChannel.receive(10000);
			assertNotNull(m);
			replies.add((String) m.getPayload());
		}
		for (int i = 0; i < 100; i++) {
			assertTrue(replies.remove("Reply" + i));
		}
	}

	@Test 
	public void testGoodNetMultiplex() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port, 10);
					latch.countDown();
					int i = 0;
					Socket socket = server.accept();
					while (true) {
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						ois.readObject();
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						oos.writeObject("Reply" + (i++));
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		for (int i = 100; i < 110; i++) {
			gateway.handleMessage(MessageBuilder.withPayload("Test" + i).build());
		}
		Set<String> replies = new HashSet<String>();
		for (int i = 100; i < 110; i++) { 
			Message<?> m = replyChannel.receive(10000);
			assertNotNull(m);
			replies.add((String) m.getPayload());
		}
		for (int i = 0; i < 10; i++) {
			assertTrue(replies.remove("Reply" + i));
		}
		done.set(true);
	}

	@Test 
	public void testGoodNetTimeout() throws Exception {
		final int port = SocketTestUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					int i = 0;
					Socket socket = server.accept();
					while (true) {
						ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
						ois.readObject();
						ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
						Thread.sleep(1000);
						oos.writeObject("Reply" + (i++));
					}
				} catch (Exception e) {
					if (!done.get()) {
						e.printStackTrace();
					}
				}
			}
		});
		AbstractConnectionFactory ccf = new TcpNetClientConnectionFactory("localhost", port);
		ccf.setSerializer(new DefaultSerializer());
		ccf.setDeserializer(new DefaultDeserializer());
		ccf.setSoTimeout(10000);
		ccf.setSingleUse(false);
		ccf.start();
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		final TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		gateway.setRequestTimeout(1);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		@SuppressWarnings("unchecked")
		Future<Integer>[] results = new Future[2];
		for (int i = 0; i < 2; i++) {
			final int j = i;
			results[j] = (Executors.newSingleThreadExecutor().submit(new Callable<Integer>(){
				public Integer call() throws Exception {
					gateway.handleMessage(MessageBuilder.withPayload("Test" + j).build());
					return 0;
				}
			}));
		}
		Set<String> replies = new HashSet<String>();
		int timeouts = 0;
		for (int i = 0; i < 2; i++) { 
			try {
				results[i].get();
			} catch (InterruptedException e) {
				
			} catch (ExecutionException e) {
				if (timeouts > 0) {
					fail("Unexpected " + e.getMessage());
				} else {
					assertNotNull(e.getCause());
					assertTrue(e.getCause() instanceof MessageTimeoutException);
				}
				timeouts++;
				continue;
			}
			Message<?> m = replyChannel.receive(10000);
			assertNotNull(m);
			replies.add((String) m.getPayload());
		}
		if (timeouts < 1) {
			fail("Expected ExecutionException");
		}
		for (int i = 0; i < 1; i++) {
			assertTrue(replies.remove("Reply" + i));
		}
		done.set(true);
	}


}
