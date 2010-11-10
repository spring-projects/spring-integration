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

package org.springframework.integration.ftp.outbound;

import org.springframework.beans.factory.config.AbstractFactoryBean;

import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.ftp.client.AbstractFtpClientFactory;
import org.springframework.integration.ftp.client.DefaultFtpClientFactory;
import org.springframework.integration.ftp.client.QueuedFtpClientPool;

/**
 * A factory bean implementation that handles constructing an outbound FTP
 * adapter.
 * 
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpSendingMessageHandlerFactoryBean extends AbstractFactoryBean<FtpSendingMessageHandler> {

	protected int port;

	protected String username;

	protected String password;

	protected String host;

	protected String remoteDirectory;

	private String charset;

	protected int clientMode;

	private int fileType;

	private FileNameGenerator fileNameGenerator;


	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	public Class<? extends FtpSendingMessageHandler> getObjectType() {
		return FtpSendingMessageHandler.class;
	}

	protected AbstractFtpClientFactory<?> clientFactory() {
		DefaultFtpClientFactory defaultFtpClientFactory = new DefaultFtpClientFactory();
		defaultFtpClientFactory.setHost(this.host);
		defaultFtpClientFactory.setPort(this.port);
		defaultFtpClientFactory.setUsername(this.username);
		defaultFtpClientFactory.setPassword(this.password);
		defaultFtpClientFactory.setRemoteWorkingDirectory(this.remoteDirectory);
		defaultFtpClientFactory.setClientMode(this.clientMode);
		defaultFtpClientFactory.setFileType(this.fileType);
		return defaultFtpClientFactory;
	}

	@Override
	protected FtpSendingMessageHandler createInstance() throws Exception {
		AbstractFtpClientFactory<?> defaultFtpClientFactory = clientFactory();
		QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, defaultFtpClientFactory);
		FtpSendingMessageHandler ftpSendingMessageHandler = new FtpSendingMessageHandler(
				queuedFtpClientPool);
		ftpSendingMessageHandler.setFileNameGenerator(this.fileNameGenerator);
		if (this.charset != null) {
			ftpSendingMessageHandler.setCharset(this.charset);
		}
		ftpSendingMessageHandler.afterPropertiesSet();
		return ftpSendingMessageHandler;
	}

}
