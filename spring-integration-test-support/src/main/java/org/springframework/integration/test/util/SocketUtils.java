/*
 * Copyright 2002-2017 the original author or authors.
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

package org.springframework.integration.test.util;

import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import javax.net.ServerSocketFactory;

import org.springframework.util.Assert;

/**
 * Contains several socket-specific utility methods. For example, you may have test cases
 * that require an open port. Rather than hard-coding the relevant port, it will be better
 * to use methods from this utility class to automatically select an open port, therefore
 * improving the portability of your test-cases across systems.
 *
 * @deprecated - it's generally better to set the server port to 0; let the operating
 * system choose a port; wait until the server starts (see TestingUtilities in the ip
 * module for an example), then set the port on the client factory.
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @since 2.2
 */
@Deprecated
public final class SocketUtils {

	public static final int DEFAULT_PORT_RANGE_MIN = 10000;

	public static final int DEFAULT_PORT_RANGE_MAX = 60000;

	/**
	 * The constructor is intentionally public. In several test cases you may have
	 * the need to use the methods of this class multiple times from within your
	 * Spring Application Context XML file using SpEL. Of course you can do:
	 * <pre class="code">
	 * {@code
	 * ...port="#{T(org.springframework.integration.test.util.SocketUtils).findAvailableServerSocket(12000)}"
	 * }
	 * </pre>
	 * But unfortunately, you would need to repeat the package for each usage.
	 * This will be acceptable for single use, but if you need to invoke the
	 * methods numerous time, you may instead want to do this:
	 * <pre class="code">
	 * {@code
	 * <bean id="tcpIpUtils" class="org.springframework.integration.test.util.SocketUtils" />
	 *
	 * ...port="#{tcpIpUtils.findAvailableServerSocket(12000)}"
	 * }
	 * </pre>
	 */
	private SocketUtils() {
	}

	/**
	 * Determines a free available server socket (port) using the 'seed' value as
	 * the starting port. The utility methods will probe for 200 sockets but will
	 * return as soon an open port is found.
	 * @param seed The starting port, which must not be negative.
	 * @return An available port number
	 * @throws IllegalStateException when no open port was found.
	 */
	public static int findAvailableServerSocket(int seed) {
		final List<Integer> openPorts = findAvailableServerSockets(seed, 1);
		return openPorts.get(0);
	}

	/**
	 * Determines a free available server socket (port) using the 'seed' value as
	 * the starting port. The utility methods will probe for 200 sockets but will
	 * return as soon an open port is found.
	 * @param seed                   The starting port, which must not be negative.
	 * @param numberOfRequestedPorts How many open ports shall be retrieved?
	 * @return A list containing the requested number of open ports
	 * @throws IllegalStateException when no open port was found.
	 */
	public static List<Integer> findAvailableServerSockets(int seed, int numberOfRequestedPorts) {

		Assert.isTrue(seed >= 0, "'seed' must not be negative");
		Assert.isTrue(numberOfRequestedPorts > 0, "'numberOfRequestedPorts' must not be negative");

		final List<Integer> openPorts = new ArrayList<Integer>(numberOfRequestedPorts);

		for (int i = seed; i < seed + 200;  i = i == 0 ? i : i + 1) {
			try {
				ServerSocket sock = ServerSocketFactory.getDefault()
						.createServerSocket(i, 1, InetAddress.getByName("localhost"));
				sock.close();
				openPorts.add(i == 0 ? sock.getLocalPort() : i);

				if (openPorts.size() == numberOfRequestedPorts) {
					return openPorts;
				}

			}
			catch (Exception e) {
			}
		}

		throw new IllegalStateException(String.format("Cannot find a free server socket (%s requested)",
				numberOfRequestedPorts));
	}

	/**
	 * Determines a free available server socket (port) using an automatically
	 * chosen start seed port.
	 * @return An available port number
	 * @throws IllegalStateException when no open port was found.
	 */
	public static int findAvailableServerSocket() {
		int seed = getRandomSeedPort();
		return findAvailableServerSocket(seed);
	}

	/**
	 * Determines a free available Udp socket (port) using the 'seed' value as
	 * the starting port. The utility methods will probe for 200 sockets but will
	 * return as soon an open port is found.
	 * @param seed The starting port, which must not be negative.
	 * @return An available port number
	 * @throws IllegalStateException when no open port was found.
	 */
	public static int findAvailableUdpSocket(int seed) {
		final List<Integer> openPorts = findAvailableUdpSockets(seed, 1);
		return openPorts.get(0);
	}

	/**
	 * Determines free available udp socket(s) (port) using the 'seed' value as
	 * the starting port. The utility methods will probe for 200 sockets but will
	 * return as soon an open port is found.
	 * @param seed                   The starting port, which must not be negative.
	 * @param numberOfRequestedPorts How many open ports shall be retrieved?
	 * @return A list containing the requested number of open ports
	 * @throws IllegalStateException when no open port was found.
	 */
	public static List<Integer> findAvailableUdpSockets(int seed, int numberOfRequestedPorts) {

		Assert.isTrue(seed >= 0, "'seed' must not be negative");
		Assert.isTrue(numberOfRequestedPorts > 0, "'numberOfRequestedPorts' must not be negative");

		final List<Integer> openPorts = new ArrayList<Integer>(numberOfRequestedPorts);

		for (int i = seed; i < seed + 200; i++) {
			try {
				DatagramSocket sock = new DatagramSocket(i, InetAddress.getByName("localhost"));
				sock.close();
				Thread.sleep(100);

				openPorts.add(i);

				if (openPorts.size() == numberOfRequestedPorts) {
					return openPorts;
				}

			}
			catch (Exception e) {
			}
		}

		throw new IllegalStateException(String.format("Cannot find a free server socket (%s requested)",
				numberOfRequestedPorts));
	}

	/**
	 * Determines a free available Udp socket using an automatically
	 * chosen start seed port.
	 * @return An available port number
	 * @throws IllegalStateException when no open port was found.
	 */
	public static int findAvailableUdpSocket() {
		int seed = getRandomSeedPort();
		return findAvailableUdpSocket(seed);
	}

	/**
	 * Determines a random seed port number within the port range
	 * {@value #DEFAULT_PORT_RANGE_MIN} and {@value #DEFAULT_PORT_RANGE_MAX}.
	 * @return A number with the the specified range
	 */
	public static int getRandomSeedPort() {
		return new Random().nextInt(DEFAULT_PORT_RANGE_MAX - DEFAULT_PORT_RANGE_MIN + 1) + DEFAULT_PORT_RANGE_MIN;
	}

}
