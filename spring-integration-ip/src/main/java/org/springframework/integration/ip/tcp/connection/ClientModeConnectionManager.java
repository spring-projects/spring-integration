/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import java.util.Objects;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.util.Assert;

/**
 * Intended to be run on a schedule, simply gets the connection
 * from a client connection factory each time it is run.
 * If no connection exists (or it has been closed), the
 * connection factory will create a new one (if possible).
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class ClientModeConnectionManager implements Runnable {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final AbstractConnectionFactory clientConnectionFactory;

	private volatile TcpConnection lastConnection;

	/**
	 * @param clientConnectionFactory The connection factory.
	 */
	public ClientModeConnectionManager(
			AbstractConnectionFactory clientConnectionFactory) {
		Assert.notNull(clientConnectionFactory, "Connection factory cannot be null");
		this.clientConnectionFactory = clientConnectionFactory;
	}

	@Override
	public void run() {
		synchronized (this.clientConnectionFactory) {
			try {
				TcpConnection connection = this.clientConnectionFactory.getConnection();
				if (!Objects.equals(connection, this.lastConnection)) {
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("Connection " + connection.getConnectionId() + " established");
					}
					this.lastConnection = connection;
				}
				else {
					if (this.logger.isTraceEnabled()) {
						this.logger.trace("Connection " + connection.getConnectionId() + " still OK");
					}
				}
			}
			catch (Exception ex) {
				this.logger.error("Could not establish connection using " + this.clientConnectionFactory, ex);
			}
		}
	}

	public boolean isConnected() {
		return this.lastConnection != null && this.lastConnection.isOpen();
	}

}
