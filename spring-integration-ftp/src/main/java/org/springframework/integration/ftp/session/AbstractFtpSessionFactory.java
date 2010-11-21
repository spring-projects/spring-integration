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

import java.io.IOException;
import java.net.SocketException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPClientConfig;
import org.apache.commons.net.ftp.FTPReply;

import org.springframework.integration.MessagingException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for FTP SessionFactory implementations.
 *
 * @author Iwein Fuld
 */
public abstract class AbstractFtpSessionFactory<T extends FTPClient> implements SessionFactory {

	public static final String DEFAULT_REMOTE_WORKING_DIRECTORY = "/";


	private final Log logger = LogFactory.getLog(this.getClass());

	protected FTPClientConfig config;

	protected String username;

	protected String host;

	protected String password;

	protected int port = FTP.DEFAULT_PORT;

	protected String remoteWorkingDirectory = DEFAULT_REMOTE_WORKING_DIRECTORY;


  
	protected int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

	protected int fileType = FTP.BINARY_FILE_TYPE;


	/**
	 * File types defined by {@link org.apache.commons.net.ftp.FTP} constants.
	 * <br>
	 * ASCII_FILE_TYPE=0; <br> 
	 * EBCDIC_FILE_TYPE=1; <br>
	 * BINARY_FILE_TYPE=3 (DEFAULT);<br>
	 * LOCAL_FILE_TYPE=4;
	 *  
	 * @param fileType
	 */
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
	 * ACTIVE_LOCAL_DATA_CONNECTION_MODE = 0 <br>
	 * A constant indicating the FTP session is expecting all transfers
	 * to occur between the client (local) and server and that the server
	 * should connect to the client's data port to initiate a data transfer.
	 * This is the default data connection mode when and FTPClient instance
	 * is created.
	 * PASSIVE_LOCAL_DATA_CONNECTION_MODE = 2 <br>
	 * A constant indicating the FTP session is expecting all transfers
	 * to occur between the client (local) and server and that the server
	 * is in passive mode, requiring the client to connect to the
	 * server's data port to initiate a transfer.
	 */
	public void setClientMode(int clientMode) {
		Assert.isTrue(clientMode == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE || 
				clientMode == FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE, 
				"Only local modes are supported. Was: " + clientMode);
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

	public Session getSession() {
		try {
			T client = this.createClient();
			if (client == null) {
				return null;
			}
			return new FtpSession(client);
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to create FTPClient", e);
		}
	}

	protected T createClient() throws SocketException, IOException { 
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

		this.updateClientMode(client);
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
	private void updateClientMode(FTPClient client) {
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
