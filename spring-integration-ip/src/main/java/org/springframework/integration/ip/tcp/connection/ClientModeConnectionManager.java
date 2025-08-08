/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

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
 * @author Christian Tzolov
 *
 * @since 2.1
 *
 */
public class ClientModeConnectionManager implements Runnable {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Lock lock = new ReentrantLock();

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
		this.lock.lock();
		try {
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
		finally {
			this.lock.unlock();
		}
	}

	public boolean isConnected() {
		return this.lastConnection != null && this.lastConnection.isOpen();
	}

}
