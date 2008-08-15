/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;

import org.springframework.util.Assert;

/**
 * FTPClientPool implementation based on a Queue. This implementation has a
 * default pool size of 5, but this is configurable with a constructor argument.
 * 
 * @author Iwein Fuld
 */
public class QueuedFTPClientPool implements FTPClientPool {

	private static final int DEFAULT_POOL_SIZE = 5;

	private final Queue<FTPClient> pool;

	private volatile FTPClientConfig config;

	private volatile String host;

	private volatile int port = FTP.DEFAULT_PORT;

	private volatile String user;

	private volatile String pass;

	private volatile FTPClientFactory factory = new DefaultFactory();

	private final Log log = LogFactory.getLog(this.getClass());

	public QueuedFTPClientPool() {
		this(DEFAULT_POOL_SIZE);
	}

	/**
	 * @param maxPoolSize the maximum size of the pool
	 */
	public QueuedFTPClientPool(int maxPoolSize) {
		pool = new ArrayBlockingQueue<FTPClient>(maxPoolSize);
	}

	public synchronized FTPClient getClient() throws SocketException, IOException {
		return pool.isEmpty() ? factory.getClient() : pool.poll();
	}

	public synchronized void releaseClient(FTPClient client) {
		if (client != null && client.isConnected()) {
			if (!pool.offer(client)) {
				try {
					client.disconnect();
				}
				catch (IOException e) {
					log.warn("Error disconnecting ftpclient", e);
				}
			}
		}
	}

	public void setConfig(FTPClientConfig config) {
		Assert.notNull(config);
		this.config = config;
	}

	public void setHost(String host) {
		Assert.hasText(host);
		this.host = host;
	}

	public void setPort(int port) {
		Assert.isTrue(port > 0, "Port number should be > 0");
		this.port = port;
	}

	public void setUser(String user) {
		Assert.hasText(user);
		this.user = user;
	}

	public void setPass(String pass) {
		Assert.notNull(pass);
		this.pass = pass;
	}

	public void setFactory(FTPClientFactory factory) {
		Assert.notNull(factory);
		this.factory = factory;
	}

	private class DefaultFactory implements FTPClientFactory {

		public FTPClient getClient() throws SocketException, IOException {
			FTPClient client = new FTPClient();
			client.configure(config);
			client.connect(host, port);
			client.login(user, pass);
			return client;
		}
	}

}
