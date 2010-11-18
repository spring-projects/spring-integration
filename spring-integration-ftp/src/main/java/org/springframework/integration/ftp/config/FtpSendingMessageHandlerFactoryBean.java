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

package org.springframework.integration.ftp.config;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.ftp.client.AbstractFtpClientFactory;
import org.springframework.integration.ftp.client.DefaultFtpClientFactory;
import org.springframework.integration.ftp.client.QueuedFtpClientPool;
import org.springframework.integration.ftp.outbound.FtpSendingMessageHandler;

/**
 * A factory bean implementation that handles constructing an outbound FTP
 * adapter.
 * 
 * @author Iwein Fuld
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class FtpSendingMessageHandlerFactoryBean extends AbstractFactoryBean<FtpSendingMessageHandler> {

	private  volatile String charset;

	private volatile FileNameGenerator fileNameGenerator;
	
	private volatile AbstractFtpClientFactory<?> clientFactory;

	public void setClientFactory(AbstractFtpClientFactory<?> clientFactory) {
		this.clientFactory = clientFactory;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	@Override
	public Class<? extends FtpSendingMessageHandler> getObjectType() {
		return FtpSendingMessageHandler.class;
	}

	protected AbstractFtpClientFactory<?> createClientFactory(){
		return new DefaultFtpClientFactory();
	}

	@Override
	protected FtpSendingMessageHandler createInstance() throws Exception {
		QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, this.clientFactory);
		FtpSendingMessageHandler ftpSendingMessageHandler = new FtpSendingMessageHandler(
				queuedFtpClientPool);
		if (this.fileNameGenerator != null){
			ftpSendingMessageHandler.setFileNameGenerator(this.fileNameGenerator);
		}
	
		if (this.charset != null) {
			ftpSendingMessageHandler.setCharset(this.charset);
		}
		ftpSendingMessageHandler.afterPropertiesSet();
		return ftpSendingMessageHandler;
	}

}
