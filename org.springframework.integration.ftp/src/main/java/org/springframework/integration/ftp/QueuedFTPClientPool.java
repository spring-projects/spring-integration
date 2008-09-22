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

package org.springframework.integration.ftp;

import java.io.IOException;
import java.net.SocketException;
import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * FTPClientPool implementation based on a Queue. This implementation has a
 * default pool size of 5, but this is configurable with a constructor argument.
 * <p>
 * This implementation pools released clients, but gives no guarantee to the
 * number of clients open at the same time.
 * 
 * @author Iwein Fuld
 */
public class QueuedFTPClientPool implements FTPClientPool {

	private static final int DEFAULT_POOL_SIZE = 5;

	private static final String DEFAULT_REMOTE_WORKING_DIRECTORY = "/";

	private final Queue<FTPClient> pool;

	private volatile FTPClientConfig config;

	private volatile String host;

	private volatile int port = FTP.DEFAULT_PORT;

	private volatile String username;

	private volatile String password;

	private volatile FTPClientFactory factory = new DefaultFactory();

	private final Log log = LogFactory.getLog(this.getClass());

	private volatile String remoteWorkingDirectory = DEFAULT_REMOTE_WORKING_DIRECTORY;

	// setters
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

	public void setUsername(String user) {
		Assert.hasText(user, "'user' should be a nonempty string");
		this.username = user;
	}

	public void setPassword(String pass) {
		Assert.notNull(pass, "password should not be null");
		this.password = pass;
	}

	public void setRemoteWorkingDirectory(String remoteWorkingDirectory) {
		Assert.notNull(remoteWorkingDirectory, "remote directory should not be null");
		this.remoteWorkingDirectory = remoteWorkingDirectory.replaceAll("^$", "/");
	}

	public void setFactory(FTPClientFactory factory) {
		Assert.notNull(factory);
		this.factory = factory;
	}

	public QueuedFTPClientPool() {
		this(DEFAULT_POOL_SIZE);
	}

	/**
	 * @param maxPoolSize the maximum size of the pool
	 */
	public QueuedFTPClientPool(int maxPoolSize) {
		pool = new ArrayBlockingQueue<FTPClient>(maxPoolSize);
	}

	/**
	 * Returns an active FTPClient connected to the configured server. When no
	 * clients are available in the queue a new client is created with the
	 * factory.
	 * 
	 * It is possible that released clients are disconnected by the remote
	 * server (@see {@link FTPClient#sendNoOp()}. In this case getClient is
	 * called recursively to obtain a client that is still alive. For this
	 * reason large pools are not recommended in poor networking conditions.
	 */
	public FTPClient getClient() throws SocketException, IOException {
		FTPClient client = pool.poll();
		if (client == null) {
			client = factory.getClient();
		}
		else {
			client = isClientAlive(client) ? client : getClient();
		}
		return client;
	}

	private boolean isClientAlive(FTPClient client) {
		try {
			if (client.sendNoOp()) {
				return true;
			}
		}
		catch (IOException e) {
			log.warn("Client [" + client + "] discarded: ", e);
		}
		return false;
	}

	public void releaseClient(FTPClient client) {
		Assert.notNull(client, "'client' cannot be null");
		if (!pool.offer(client)) {
			try {
				client.disconnect();
			}
			catch (IOException e) {
				log.warn("Error disconnecting ftpclient", e);
			}
		}
	}

	private class DefaultFactory implements FTPClientFactory {

		public FTPClient getClient() throws SocketException, IOException {
			FTPClient client = new FTPClient();
			client.configure(config);
			if (!StringUtils.hasText(username)) {
				throw new MessagingException("username is required");
			}
			client.connect(host, port);
			if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
				throw new MessagingException("Connecting to server [" + host + ":" + port
						+ "] failed, please check the connection");
			}
			if (log.isDebugEnabled()) {
				log.debug("Connected to server [" + host + ":" + port + "]");
			}
			if (!client.login(username, password)) {
				throw new MessagingException("Login failed. Please check the username and password.");
			}
			if (log.isDebugEnabled()) {
				log.debug("login successful");
			}
			client.setFileType(FTP.BINARY_FILE_TYPE);

			if (!remoteWorkingDirectory.equals(client.printWorkingDirectory())
					&& !client.changeWorkingDirectory(remoteWorkingDirectory)) {
				throw new MessagingException("Could not change directory to '" + remoteWorkingDirectory
						+ "'. Please check the path.");
			}
			if (log.isDebugEnabled()) {
				log.debug("working directory is: " + client.printWorkingDirectory());
			}
			return client;
		}
	}
}
