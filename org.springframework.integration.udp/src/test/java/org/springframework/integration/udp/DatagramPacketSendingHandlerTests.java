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

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

import org.springframework.integration.message.MessageBuilder;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class DatagramPacketSendingHandlerTests {

	@Test
	public void verifySend() throws Exception {
		final int testPort = 27816;
		byte[] buffer = new byte[8];
		final DatagramPacket receivedPacket = new DatagramPacket(buffer, buffer.length);
		final CountDownLatch latch = new CountDownLatch(1);
		Executors.newSingleThreadExecutor().execute(new Runnable() {
			public void run() {
				try {
					DatagramSocket socket = new DatagramSocket(testPort);
					socket.receive(receivedPacket);
					latch.countDown();
				}
				catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
		DatagramPacketSendingHandler handler = new DatagramPacketSendingHandler("localhost", testPort);
		String payload = "foo";
		handler.handleMessage(MessageBuilder.withPayload(payload).build());
		latch.await(3000, TimeUnit.MILLISECONDS);
		byte[] src = receivedPacket.getData();
		int length = receivedPacket.getLength();
		int offset = receivedPacket.getOffset();
		byte[] dest = new byte[length];
		System.arraycopy(src, offset, dest, 0, length);
		assertEquals(payload, new String(dest));
	}

}
