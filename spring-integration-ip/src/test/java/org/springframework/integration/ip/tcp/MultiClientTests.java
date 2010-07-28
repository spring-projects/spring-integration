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

import static org.junit.Assert.assertNotNull;
import junit.framework.Assert;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;


/**
 * 
 * For both .net. and .nio. adapters, creates a single server and 10 clients
 * and sends 3 long messages from each client to the associated server.
 * Ensures that all messages are correctly assembled and received ok.
 * 
 * @author Gary Russell
 * @since 2.0
 *
 */
public class MultiClientTests {

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testNet() throws Exception {
		final String payload = largePayload(10000); // force fragmentation 
		final TcpNetReceivingChannelAdapter adapter = 
			new TcpNetReceivingChannelAdapter(SocketUtils.findAvailableServerSocket());
		int drivers = 10;
		adapter.setPoolSize(drivers);
		adapter.setReceiveBufferSize(10000);
		QueueChannel queue = new QueueChannel(drivers * 3);
		adapter.setOutputChannel(queue);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		SocketUtils.waitListening(adapter);
		for (int i = 0; i < drivers; i++) {
			Thread t = new Thread( new Runnable() {
				public void run() {
					TcpNetSendingMessageHandler sender = new TcpNetSendingMessageHandler("localhost", adapter.getPort());
					Message<String> message = MessageBuilder.withPayload(payload).build();
					sender.handleMessage(message);
					// and again
					sender.handleMessage(message);
					// and again
					sender.handleMessage(message);
				}});
			t.setDaemon(true);
			t.start();
		}
		for (int i = 0; i < drivers * 3 ; i++) {
			Message<byte[]> messageOut = (Message<byte[]>) queue.receive(6000);
			assertNotNull(messageOut);
			Assert.assertEquals(payload, new String(messageOut.getPayload()));
		}
		adapter.stop();
	}

	@SuppressWarnings("unchecked")
	@Test @Ignore
	public void testNio() throws Exception {
		final String payload = largePayload(10000); // force fragmentation 
		final TcpNioReceivingChannelAdapter adapter = 
			new TcpNioReceivingChannelAdapter(SocketUtils.findAvailableServerSocket());
		adapter.setPoolSize(4);
		adapter.setReceiveBufferSize(10000);
		int drivers = 10;
		QueueChannel queue = new QueueChannel(drivers * 3);
		adapter.setOutputChannel(queue);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		SocketUtils.waitListening(adapter);
		for (int i = 0; i < drivers; i++) {
			Thread t = new Thread( new Runnable() {
				public void run() {
					TcpNioSendingMessageHandler sender = new TcpNioSendingMessageHandler("localhost", adapter.getPort());
					Message<String> message = MessageBuilder.withPayload(payload).build();
					sender.handleMessage(message);
					// and again
					sender.handleMessage(message);
					// and again
					sender.handleMessage(message);
				}});
			t.setDaemon(true);
			t.start();
		}
		for (int i = 0; i < drivers * 3 ; i++) {
			Message<byte[]> messageOut = (Message<byte[]>) queue.receive(6000);
			assertNotNull(messageOut);
			Assert.assertEquals(payload, new String(messageOut.getPayload()));
		}
		adapter.stop();
	}

	private String largePayload(int n) {
		StringBuilder sb = new StringBuilder(n);
		for (int i = 0; i < n; i++) {
			sb.append('x');
		}
		return sb.toString();
	}

}
