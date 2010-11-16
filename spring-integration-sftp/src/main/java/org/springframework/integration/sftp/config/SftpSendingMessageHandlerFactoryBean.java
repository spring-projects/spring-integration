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
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.sftp.outbound.SftpSendingMessageHandler;
import org.springframework.integration.sftp.session.QueuedSftpSessionPool;
import org.springframework.integration.sftp.session.SftpSessionFactory;

/**
 * Supports the construction of a MessagHandler that knows how to take inbound File objects
 * and send them to a remote destination.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class SftpSendingMessageHandlerFactoryBean implements FactoryBean<SftpSendingMessageHandler> {

	private Expression remoteDirectoryExpression;

	private volatile SftpSessionFactory sftpSessionFactory;
	
	private volatile String charset;
	
	private volatile FileNameGenerator fileNameGenerator;

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setSftpSessionFactory(SftpSessionFactory sftpSessionFactory) {
		this.sftpSessionFactory = sftpSessionFactory;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		remoteDirectory = (remoteDirectory != null) ? remoteDirectory : ""; 
		this.remoteDirectoryExpression = new LiteralExpression(remoteDirectory);
	}

	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.remoteDirectoryExpression = remoteDirectoryExpression;
	}

	public SftpSendingMessageHandler getObject() throws Exception {
		QueuedSftpSessionPool sessionPool = new QueuedSftpSessionPool(15, sftpSessionFactory);
		sessionPool.afterPropertiesSet();
		SftpSendingMessageHandler messageHandler = new SftpSendingMessageHandler(sessionPool);
		messageHandler.setRemoteDirectoryExpression(this.remoteDirectoryExpression);
		messageHandler.setCharset(this.charset);
		messageHandler.setFileNameGenerator(this.fileNameGenerator);
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
