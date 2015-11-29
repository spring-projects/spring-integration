/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ip.udp;

import org.springframework.context.Lifecycle;

import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Registry for shared unicast datagram sockets.
 *
 * @author Marcin Pilaczynski
 * @since 4.3
 */
public enum UnicastDatagramSocketRegistry implements Lifecycle {

	INSTANCE;

	//	public static final String BEAN_NAME = "UnicastDatagramSocketRegistry";
	protected final Map<String, DatagramSocket> stringDatagramSocketMap = new ConcurrentHashMap<String, DatagramSocket>();
	private volatile boolean running;

	public DatagramSocket getDatagramSocket(String datagramSocketId) throws SocketException {
		if (datagramSocketId == null || stringDatagramSocketMap.get(datagramSocketId) == null) {
			return new DatagramSocket();
		} else {
			return stringDatagramSocketMap.get(datagramSocketId);
		}
	}

	public void addDatagramSocket(String datagramSocketId, DatagramSocket datagramSocket) {
		stringDatagramSocketMap.put(datagramSocketId, datagramSocket);
	}

	@Override
	public void start() {
		running = true;
	}

	@Override
	public void stop() {
		running = false;
	}

	@Override
	public boolean isRunning() {
		return running;
	}

	public void removeDatagramSocket(String datagramSocketId) {
		stringDatagramSocketMap.remove(datagramSocketId);
	}
}
