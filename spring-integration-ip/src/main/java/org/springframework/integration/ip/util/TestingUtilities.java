/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.util;

import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.udp.inbound.AbstractInternetProtocolReceivingChannelAdapter;

/**
 * Convenience class providing methods for testing IP components.
 * Provided in the main branch so that it is available for
 * use in user test code, samples etc.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public final class TestingUtilities {

	// Non-instantiable utility class
	private TestingUtilities() {
		throw new AssertionError("Class Instantiation not allowed.");
	}

	/**
	 * Wait for a server connection factory to actually start listening before
	 * starting a test. Waits for up to 10 seconds by default.
	 * @param serverConnectionFactory The server connection factory.
	 * @param delay How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException If the server does not start listening in time.
	 */
	public static void waitListening(AbstractServerConnectionFactory serverConnectionFactory, @Nullable Long delay)
			throws IllegalStateException {

		waitListeningInternal(serverConnectionFactory::isListening, delay);
	}

	/**
	 * Wait for a server connection factory to actually start listening before
	 * starting a test. Waits for up to 10 seconds by default.
	 * @param adapter The server connection factory.
	 * @param delay How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException If the server does not start listening in time.
	 */
	public static void waitListening(AbstractInternetProtocolReceivingChannelAdapter adapter, @Nullable Long delay)
			throws IllegalStateException {

		waitListeningInternal(adapter::isListening, delay);
	}

	private static void waitListeningInternal(Supplier<Boolean> isListeningSupplier, @Nullable Long delayArg)
			throws IllegalStateException {

		Long delay = delayArg;
		if (delay == null) {
			delay = 100L;
		}
		else {
			delay = delay / 100;
		}
		int n = 0;
		while (!isListeningSupplier.get()) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e1);
			}

			if (n++ > delay) {
				throw new IllegalStateException("Server didn't start listening.");
			}
		}
	}

	/**
	 * Wait for a server connection factory to stop listening.
	 * Waits for up to 10 seconds by default.
	 * @param serverConnectionFactory The server connection factory.
	 * @param delayArg How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException If the server doesn't stop listening in time.
	 */
	public static void waitStopListening(AbstractServerConnectionFactory serverConnectionFactory,
			@Nullable Long delayArg)
			throws IllegalStateException {

		Long delay = delayArg;
		if (delay == null) {
			delay = 100L;
		}
		else {
			delay = delay / 100;
		}
		int n = 0;
		while (serverConnectionFactory.isListening()) {
			try {
				Thread.sleep(100);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
			if (n++ > delay) {
				throw new IllegalStateException("Server didn't stop listening.");
			}
		}
	}

	/**
	 * Wait for up to 10 seconds for the connection factory to have the specified number
	 * of connections.
	 * @param factory The factory.
	 * @param n The required number of connections.
	 * @throws InterruptedException if interrupted.
	 * @throws IllegalStateException if the count does not match.
	 */
	public static void waitUntilFactoryHasThisNumberOfConnections(AbstractConnectionFactory factory, int n)
			throws InterruptedException {

		int timer = 0;
		while (timer < 10000) {
			if (factory.getOpenConnectionIds().size() == n) {
				return;
			}
			Thread.sleep(100);
			timer += 100;
		}
		throw new IllegalStateException("Connections=" + factory.getOpenConnectionIds().size() + "wanted=" + n);
	}

}
