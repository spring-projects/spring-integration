/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.udp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;

import org.junit.Ignore;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class DatagramPacketReceivingChannelAdapterTests {

	@Test
	@Ignore
	public void receive() throws IOException {
		int testPort = 23487;
		QueueChannel output = new QueueChannel();
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.afterPropertiesSet();
		DatagramPacketReceivingChannelAdapter adapter = new DatagramPacketReceivingChannelAdapter(testPort);
		adapter.setTaskScheduler(taskScheduler);
		adapter.setOutputChannel(output);
		adapter.afterPropertiesSet();
		DatagramSocket socket = new DatagramSocket();
		byte[] bytes = "foo".getBytes("UTF-8");
		DatagramPacket packet = new DatagramPacket(bytes, bytes.length);
		packet.setSocketAddress(new InetSocketAddress(testPort));
		socket.send(packet);
		socket.close();
		Message<?> message = output.receive(3000);
		assertNotNull(message);
		assertEquals("foo", new String((byte[]) message.getPayload(), "UTF-8"));
	}

}
