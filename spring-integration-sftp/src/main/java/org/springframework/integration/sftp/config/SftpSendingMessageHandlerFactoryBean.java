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

package org.springframework.integration.sftp.config;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.sftp.outbound.SftpSendingMessageHandler;
import org.springframework.integration.sftp.session.QueuedSftpSessionPool;
import org.springframework.integration.sftp.session.SftpSessionFactoryBean;
import org.springframework.integration.sftp.session.SftpSessionUtils;

/**
 * Supports the construction of a MessagHandler that knows how to take inbound File objects
 * and send them to a remote destination.
 *
 * @author Josh Long
 */
class SftpSendingMessageHandlerFactoryBean implements FactoryBean<SftpSendingMessageHandler> {

	private String host;

	private String keyFile;

	private String keyFilePassword;

	private String password;

	private Expression remoteDirectoryExpression;

	private String username;

	private int port;

	private String charset;


	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setHost(final String host) {
		this.host = host;
	}

	public void setKeyFile(final String keyFile) {
		this.keyFile = keyFile;
	}

	public void setKeyFilePassword(final String keyFilePassword) {
		this.keyFilePassword = keyFilePassword;
	}

	public void setUsername(final String username) {
		this.username = username;
	}

	public void setPassword(final String password) {
		this.password = password;
	}

	public void setPort(final int port) {
		this.port = port;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		remoteDirectory = (remoteDirectory != null) ? remoteDirectory : ""; 
		this.remoteDirectoryExpression = new LiteralExpression(remoteDirectory);
	}

	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.remoteDirectoryExpression = remoteDirectoryExpression;
	}

	public SftpSendingMessageHandler getObject() throws Exception {
		SftpSessionFactoryBean sessionFactory = SftpSessionUtils.buildSftpSessionFactory(
				this.host, this.password, this.username, this.keyFile, this.keyFilePassword, this.port);
		QueuedSftpSessionPool sessionPool = new QueuedSftpSessionPool(15, sessionFactory);
		sessionPool.afterPropertiesSet();
		SftpSendingMessageHandler messageHandler = new SftpSendingMessageHandler(sessionPool);
		messageHandler.setRemoteDirectoryExpression(this.remoteDirectoryExpression);
		messageHandler.setCharset(this.charset);
		messageHandler.afterPropertiesSet();
		return messageHandler;
	}

	public Class<? extends SftpSendingMessageHandler> getObjectType() {
		return SftpSendingMessageHandler.class;
	}

	public boolean isSingleton() {
		return false;
	}

}
