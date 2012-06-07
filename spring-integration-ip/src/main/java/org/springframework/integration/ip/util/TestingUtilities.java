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
package org.springframework.integration.ip.util;

import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;

/**
 * Convenience class providing methods for testing IP components.
 * Provided in the main branch so that it is available for
 * use in user test code, samples etc.
 *
 * @author Gary Russell
 * @since 2.2
 *
 */
public class TestingUtilities {

	/**
	 * Wait for a server connection factory to actually start listening before
	 * starting a test. Waits for up to 10 seconds by default.
	 * @param serverConnectionFactory The server connection factory.
	 * @param delay How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException
	 */
	public static void waitListening(AbstractServerConnectionFactory serverConnectionFactory, Long delay)
		throws IllegalStateException {
		if (delay == null) {
			delay = 100L;
		}
		else {
			delay = delay / 100;
		}
		int n = 0;
		while (!serverConnectionFactory.isListening()) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e1) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e1);
			}

			if (n++ > delay) {
				throw new IllegalStateException ("Server didn't start listening.");
			}
		}
	}

	/**
	 * Wait for a server connection factory to stop listening.
	 * Waits for up to 10 seconds by default.
	 * @param serverConnectionFactory The server connection factory.
	 * @param delay How long to wait in milliseconds; default 10000 (10 seconds) if null.
	 * @throws IllegalStateException
	 */
	public static void waitStopListening(AbstractServerConnectionFactory serverConnectionFactory, Long delay)
			throws IllegalStateException {
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
			catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(e);
			}
			if (n++ > 200) {
				throw new IllegalStateException ("Server didn't stop listening.");
			}
		}
	}

}
