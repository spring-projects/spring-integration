/*
 * Copyright 2002-2012 the original author or authors.
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.net.ServerSocketFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.DefaultSerializer;
import org.springframework.integration.Message;
import org.springframework.integration.MessageTimeoutException;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpNetClientConnectionFactory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.SocketUtils;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGatewayTests {

	private final Log logger = LogFactory.getLog(this.getClass());

	@Test
	public void testGoodNetSingle() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
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
		ccf.start();
		assertTrue(latch.await(10000, TimeUnit.MILLISECONDS));
		TcpOutboundGateway gateway = new TcpOutboundGateway();
		gateway.setConnectionFactory(ccf);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		// check the default remote timeout
		assertEquals(Long.valueOf(10000), TestUtils.getPropertyValue(gateway, "remoteTimeout", Long.class));
		gateway.setSendTimeout(123);
		// ensure this also changed the remote timeout
		assertEquals(Long.valueOf(123), TestUtils.getPropertyValue(gateway, "remoteTimeout", Long.class));
		gateway.setRemoteTimeout(60000);
		gateway.setSendTimeout(61000);
		// ensure this did NOT change the remote timeout
		assertEquals(Long.valueOf(60000), TestUtils.getPropertyValue(gateway, "remoteTimeout", Long.class));
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
		final int port = SocketUtils.findAvailableServerSocket();
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
		final int port = SocketUtils.findAvailableServerSocket();
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

	/**
	 * Sends 2 concurrent messages on a shared connection. The GW single threads
	 * these requests. The first will timeout; the second should receive its
	 * own response, not that for the first.
	 * @throws Exception
	 */
	@Test
	public void testGoodNetGWTimeout() throws Exception {
		final int port = SocketUtils.findAvailableServerSocket();
		final CountDownLatch latch = new CountDownLatch(1);
		final AtomicBoolean done = new AtomicBoolean();
		/*
		 * The payload of the last message received by the remote side;
		 * used to verify the correct response is received.
		 */
		final AtomicReference<String> lastReceived = new AtomicReference<String>();
		final CountDownLatch serverLatch = new CountDownLatch(2);

		Executors.newSingleThreadExecutor().execute(new Runnable() {

			public void run() {
				try {
					ServerSocket server = ServerSocketFactory.getDefault().createServerSocket(port);
					latch.countDown();
					int i = 0;
					while (!done.get()) {
						Socket socket = server.accept();
						i++;
						while (!socket.isClosed()) {
							try {
								ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());
								String request = (String) ois.readObject();
								logger.debug("Read " + request);
								ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
								if (i < 2) {
									Thread.sleep(1000);
								}
								oos.writeObject(request.replace("Test", "Reply"));
								logger.debug("Replied to " + request);
								lastReceived.set(request);
								serverLatch.countDown();
							}
							catch (IOException e) {
								socket.close();
							}
						}
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
		gateway.setRequestTimeout(Integer.MAX_VALUE);
		QueueChannel replyChannel = new QueueChannel();
		gateway.setRequiresReply(true);
		gateway.setOutputChannel(replyChannel);
		gateway.setRemoteTimeout(500);
		@SuppressWarnings("unchecked")
		Future<Integer>[] results = new Future[2];
		for (int i = 0; i < 2; i++) {
			final int j = i;
			results[j] = (Executors.newSingleThreadExecutor().submit(new Callable<Integer>() {
				public Integer call() throws Exception {
					// increase the timeout after the first send
					if (j > 0) {
						gateway.setRemoteTimeout(5000);
					}
					gateway.handleMessage(MessageBuilder.withPayload("Test" + j).build());
					return j;
				}
			}));
			Thread.sleep(50);
		}
		// wait until the server side has processed both requests
		assertTrue(serverLatch.await(10, TimeUnit.SECONDS));
		List<String> replies = new ArrayList<String>();
		int timeouts = 0;
		for (int i = 0; i < 2; i++) {
			try {
				int result = results[i].get();
				String reply = (String) replyChannel.receive(1000).getPayload();
				logger.debug(i + " got " + result + " " + reply);
				replies.add(reply);
			} catch (ExecutionException e) {
				if (timeouts >= 2) {
					fail("Unexpected " + e.getMessage());
				} else {
					assertNotNull(e.getCause());
					assertTrue(e.getCause() instanceof MessageTimeoutException);
				}
				timeouts++;
				continue;
			}
		}
		assertEquals("Expected exactly one ExecutionException", 1, timeouts);
		assertEquals(1, replies.size());
		assertEquals(lastReceived.get().replace("Test", "Reply"), replies.get(0));
		done.set(true);
		assertEquals(0, TestUtils.getPropertyValue(gateway, "pendingReplies", Map.class).size());
	}

}
