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

package org.springframework.integration.ftp.client;

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import org.springframework.integration.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * base class for the other {@link org.springframework.integration.ftp.client.FtpClientFactory} implementations.
 * Most of this came out of the {@link DefaultFtpClientFactory} and was refactored into a base class
 *
 * @author Iwein Fuld
 */
abstract public class AbstractFtpClientFactory<T extends FTPClient> implements FtpClientFactory<T> {

	private static final Log logger = LogFactory.getLog(FtpClientFactory.class);

	private static final String DEFAULT_REMOTE_WORKING_DIRECTORY = "/";


	protected FTPClientConfig config;

	protected String username;

	protected String host;

	protected String password;

	protected int port = FTP.DEFAULT_PORT;

	protected String remoteWorkingDirectory = DEFAULT_REMOTE_WORKING_DIRECTORY;

	protected int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

	protected int fileType = FTP.BINARY_FILE_TYPE;


	public void setFileType(int fileType) {
		this.fileType = fileType;
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

	/**
	 * Set client mode, for example
	 * <code>FTPClient.ACTIVE_LOCAL_CONNECTION_MODE</code> (default)
	 * Only local modes are supported.
	 */
	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
	}

	protected abstract T createSingleInstanceOfClient();

	/**
	 * this is a hook to setup the state of the {@link org.apache.commons.net.ftp.FTPClient} impl *after* the
	 * implementation's {@link org.apache.commons.net.ftp.FTPClient#connect(String)} method's been called but before any
	 * action's been taken.
	 *
	 * @param t the ftp client instance on which to act
	 * @throws IOException if anything should go wrong
	 */
	protected void onAfterConnect(T t) throws IOException {
		// NOOP
	}

	public T getClient() throws SocketException, IOException {
		T client = createSingleInstanceOfClient();
		client.configure(config);

		if (!StringUtils.hasText(username)) {
			throw new MessagingException("username is required");
		}

		client.connect(host);
		onAfterConnect(client);

		if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
			throw new MessagingException("Connecting to server [" + host + ":" +
					port + "] failed, please check the connection");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("Connected to server [" + host + ":" + port + "]");
		}

		if (!client.login(username, password)) {
			throw new MessagingException(
					"Login failed. Please check the username and password.");
		}

		setClientMode(client);
		client.setFileType(this.fileType);

		if (logger.isDebugEnabled()) {
			logger.debug("login successful");
		}

		if (!remoteWorkingDirectory.equals(client.printWorkingDirectory()) &&
				!client.changeWorkingDirectory(remoteWorkingDirectory)) {
			throw new MessagingException("Could not change directory to '" +
					remoteWorkingDirectory + "'. Please check the path.");
		}

		if (logger.isDebugEnabled()) {
			logger.debug("working directory is: " +
					client.printWorkingDirectory());
		}
		return client;
	}

	/**
	 * Sets the mode of the connection. Only local modes are supported.
	 */
	protected void setClientMode(FTPClient client) {
		switch (clientMode) {
			case FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE:
				client.enterLocalActiveMode();
				break;
			case FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE:
				client.enterLocalPassiveMode();
				break;
			default:
				break;
		}
	}

}
