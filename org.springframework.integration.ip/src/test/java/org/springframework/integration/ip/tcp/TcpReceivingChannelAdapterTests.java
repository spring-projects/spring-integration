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

import org.junit.Test;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.integration.ip.AbstractInternetProtocolReceivingChannelAdapter;
import org.springframework.integration.ip.util.SocketUtils;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 *
 */
public class TcpReceivingChannelAdapterTests {

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter#run()}.
	 */
	@Test
	public void testNet() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableServerSocket();
		AbstractInternetProtocolReceivingChannelAdapter adapter = new TcpNetReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Thread.sleep(2000); // wait for server to start listening
		SocketUtils.testSendLength(port, null); //sends 2 copies of TEST_STRING twice
		Thread.sleep(2000); // wait for asynch processing
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				new String((byte[])message.getPayload()));
		message = channel.receive(0);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				new String((byte[])message.getPayload()));
		adapter.stop();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter#run()}.
	 * Verifies operation of custom message formats.
	 */
	@Test
	public void testNetCustom() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableServerSocket();
		TcpNetReceivingChannelAdapter adapter = new TcpNetReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.setCustomSocketReaderClassName("org.springframework.integration.ip.tcp.CustomNetSocketReader");
		adapter.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Thread.sleep(2000); // wait for server to start listening
		SocketUtils.testSendStxEtx(port, null); //sends 2 copies of TEST_STRING twice
		Thread.sleep(4000); // wait for asynch processing
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String((byte[])message.getPayload()));
		message = channel.receive(0);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String((byte[])message.getPayload()));
		adapter.stop();
	}


	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter#run()}.
	 */
	@Test
	public void testNio() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableServerSocket();
		TcpNioReceivingChannelAdapter adapter = new TcpNioReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Thread.sleep(2000); // wait for server to start listening
		SocketUtils.testSendLength(port, null); //sends 2 copies of TEST_STRING twice
		Thread.sleep(2000); // wait for asynch processing
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				new String((byte[])message.getPayload()));
		message = channel.receive(0);
		assertNotNull(message);
		assertEquals(SocketUtils.TEST_STRING + SocketUtils.TEST_STRING, 
				new String((byte[])message.getPayload()));
		adapter.stop();
	}

	/**
	 * Test method for {@link org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter#run()}.
	 * Verifies operation of custom message formats.	 */
	@Test
	public void testNioCustom() throws Exception {
		QueueChannel channel = new QueueChannel(2);
		int port = SocketUtils.findAvailableServerSocket();
		TcpNioReceivingChannelAdapter adapter = new TcpNioReceivingChannelAdapter(port);
		adapter.setOutputChannel(channel);
		adapter.setCustomSocketReaderClassName("org.springframework.integration.ip.tcp.CustomNioSocketReader");
		adapter.setMessageFormat(MessageFormats.FORMAT_CUSTOM);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		adapter.setTaskScheduler(taskScheduler);
		adapter.start();
		Thread.sleep(2000); // wait for server to start listening
		SocketUtils.testSendStxEtx(port, null); //sends 2 copies of TEST_STRING twice
		Thread.sleep(4000); // wait for asynch processing
		Message<?> message = channel.receive(0);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String((byte[])message.getPayload()));
		message = channel.receive(0);
		assertNotNull(message);
		assertEquals("\u0002" + SocketUtils.TEST_STRING + SocketUtils.TEST_STRING + "\u0003", 
				new String((byte[])message.getPayload()));
		adapter.stop();
	}

}
