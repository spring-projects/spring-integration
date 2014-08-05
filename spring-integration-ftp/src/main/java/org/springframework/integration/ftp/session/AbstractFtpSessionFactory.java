/*
 * Copyright 2002-2014 the original author or authors.
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
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * Base class for FTP SessionFactory implementations.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public abstract class AbstractFtpSessionFactory<T extends FTPClient> implements SessionFactory<FTPFile> {

	public static final String DEFAULT_REMOTE_WORKING_DIRECTORY = "/";

	private final Log logger = LogFactory.getLog(this.getClass());

	protected FTPClientConfig config;

	protected String username;

	protected String host;

	protected String password;

	protected int port = FTP.DEFAULT_PORT;

	protected int bufferSize = 2048; //see https://issues.apache.org/jira/browse/NET-207

	protected int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

	protected int fileType = FTP.BINARY_FILE_TYPE;

	protected String controlEncoding = FTP.DEFAULT_CONTROL_ENCODING;

	private Integer connectTimeout;

	private Integer defaultTimeout;

	private Integer dataTimeout;


	/**
	 * File types defined by {@link org.apache.commons.net.ftp.FTP} constants:
	 * <ul>
	 * <li>{@link org.apache.commons.net.ftp.FTP#ASCII_FILE_TYPE}</li>
	 * <li>{@link org.apache.commons.net.ftp.FTP#EBCDIC_FILE_TYPE}</li>
	 * <li>{@link org.apache.commons.net.ftp.FTP#BINARY_FILE_TYPE} (DEFAULT)</li>
	 * <li>{@link org.apache.commons.net.ftp.FTP#LOCAL_FILE_TYPE}</li>
	 * </ul>
	 * @param fileType The file type.
	 */
	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	public void setControlEncoding(String controlEncoding) {
		Assert.hasText(controlEncoding);
		this.controlEncoding = controlEncoding;
	}

	public void setConfig(FTPClientConfig config) {
		Assert.notNull(config);
		this.config = config;
	}

	public void setBufferSize(int bufferSize) {
		this.bufferSize = bufferSize;
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
	 *
	 * @param clientMode The client mode.
	 */
	public void setClientMode(int clientMode) {
		Assert.isTrue(clientMode == FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE ||
				clientMode == FTPClient.PASSIVE_LOCAL_DATA_CONNECTION_MODE,
				"Only local modes are supported. Was: " + clientMode);
		this.clientMode = clientMode;
	}

	/**
	 * Set the connect timeout for the socket.
	 * @param connectTimeout the timeout
	 */
	public void setConnectTimeout(int connectTimeout) {
		 this.connectTimeout = connectTimeout;
	}

	/**
	 * Set the (socket option) timeout on the command socket.
	 * @param defaultTimeout the timeout.
	 */
	public void setDefaultTimeout(int defaultTimeout) {
		this.defaultTimeout = defaultTimeout;
	}

	/**
	 * Set the (socket option) timeout on the data connection.
	 * @param dataTimeout the timeout.
	 */
	public void setDataTimeout(int dataTimeout) {
		this.dataTimeout = dataTimeout;
	}

	@Override
	public FtpSession getSession() {
		try {
			return new FtpSession(this.createClient());
		}
		catch (Exception e) {
			throw new IllegalStateException("failed to create FTPClient", e);
		}
	}

	private T createClient() throws SocketException, IOException {
		final T client = this.createClientInstance();
		Assert.notNull(client, "client must not be null");
		client.configure(this.config);
		Assert.hasText(this.username, "username is required");
		if (this.connectTimeout != null) {
			client.setConnectTimeout(this.connectTimeout);
		}
		if (this.defaultTimeout != null) {
			client.setDefaultTimeout(this.defaultTimeout);
		}
		if (this.dataTimeout != null) {
			client.setDataTimeout(this.dataTimeout);
		}

		this.postProcessClientBeforeConnect(client);

		// Connect
		client.connect(this.host, this.port);

		if (!FTPReply.isPositiveCompletion(client.getReplyCode())) {
			throw new MessagingException("Connecting to server [" +
					host + ":" + port + "] failed. Please check the connection.");
		}
		logger.debug("Connected to server [" + host + ":" + port + "]");

		// Login
		if (!client.login(username, password)) {
			throw new IllegalStateException("Login failed. The respponse from the server is: " +
					client.getReplyString());
		}

		this.postProcessClientAfterConnect(client);

		this.updateClientMode(client);
		client.setFileType(fileType);
		client.setBufferSize(bufferSize);
		client.setControlEncoding(controlEncoding);
		return client;
	}

	/**
	 * Sets the mode of the connection. Only local modes are supported.
	 */
	private void updateClientMode(FTPClient client) {
		switch (this.clientMode) {
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

	protected abstract T createClientInstance();

	/**
	 * Will handle additional initialization after client.connect() method was invoked,
	 * but before any action on the client has been taken
	 *
	 * @param t The client.
	 * @throws IOException Any IOException
	 */
	protected void postProcessClientAfterConnect(T t) throws IOException {
		// NOOP
	}
	/**
	 * Will handle additional initialization before client.connect() method was invoked.
	 *
	 * @param client The client.
	 * @throws IOException Any IOException.
	 */
	protected void postProcessClientBeforeConnect(T client) throws IOException {
		// NOOP
	}

}
