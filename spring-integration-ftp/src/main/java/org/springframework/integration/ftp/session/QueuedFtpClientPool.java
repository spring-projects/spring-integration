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

package org.springframework.integration.ftp.session;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;

import org.springframework.util.Assert;

import java.io.IOException;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * FtpClientPool implementation based on a Queue. This implementation has a
 * default pool size of 5, but this is configurable with a constructor argument.
 * <p/>
 * This implementation pools released clients, but gives no guarantee to the
 * number of clients open at the same time.
 *
 * @author Iwein Fuld
 */
public class QueuedFtpClientPool implements FtpClientPool {

	private static final Log logger = LogFactory.getLog(QueuedFtpClientPool.class);

	private static final int DEFAULT_POOL_SIZE = 5;


	private final Queue<FTPClient> pool;

	private final FtpClientFactory<?> factory;


	public QueuedFtpClientPool(FtpClientFactory<?> factory) {
		this(DEFAULT_POOL_SIZE, factory);
	}

	/**
	 * @param maxPoolSize the maximum size of the pool
	 */
	public QueuedFtpClientPool(int maxPoolSize, FtpClientFactory<?> factory) {
		Assert.notNull(factory, "factory must not be null");
		this.factory = factory;
		this.pool = new ArrayBlockingQueue<FTPClient>(maxPoolSize);
	}

	/**
	 * Returns an active FTPClient connected to the configured server. When no
	 * clients are available in the queue a new client is created with the
	 * factory.
	 * <p/>
	 * It is possible that released clients are disconnected by the remote
	 * server (@see {@link FTPClient#sendNoOp()}. In this case getClient is
	 * called recursively to obtain a client that is still alive. For this
	 * reason large pools are not recommended in poor networking conditions.
	 */
	public FTPClient getClient() throws SocketException, IOException {
		FTPClient client = this.pool.poll();
		if (client == null) {
			client = this.factory.getClient();
		}
		return prepareClient(client);
	}

	/**
	 * Prepares the client before it is returned through
	 * <code>getClient()</code>. The default implementation will check the
	 * connection using a noOp and replace the client with a new one if it
	 * encounters a problem.
	 * <p/>
	 * In more exotic environments subclasses can override this method to
	 * implement their own preparation strategy.
	 *
	 * @param client the unprepared client
	 * @throws SocketException
	 * @throws IOException
	 */
	protected FTPClient prepareClient(FTPClient client) throws SocketException, IOException {
		return isClientAlive(client) ? client : getClient();
	}

	private boolean isClientAlive(FTPClient client) {
		try {
			if (client.sendNoOp()) {
				return true;
			}
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("Client [" + client + "] discarded: ", e);
			}
		}
		return false;
	}

	public void releaseClient(FTPClient client) {
		if ((client != null) && !this.pool.offer(client)) {
			try {
				client.disconnect();
			}
			catch (IOException e) {
				if (logger.isWarnEnabled()) {
					logger.warn("Error disconnecting ftpclient", e);
				}
			}
		}
	}

}
