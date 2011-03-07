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

package org.springframework.integration.file.config;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.FileWritingMessageHandler;
import org.springframework.integration.support.channel.ChannelResolver;

import java.io.File;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 * @since 1.0.3
 */
public class FileWritingMessageHandlerFactoryBean implements FactoryBean<FileWritingMessageHandler>, BeanFactoryAware {

	private volatile FileWritingMessageHandler handler;

	private volatile BeanFactory beanFactory;

	private volatile File directory;

	private volatile MessageChannel outputChannel;

	private volatile ChannelResolver channelResolver;

	private volatile String charset;

	private volatile FileNameGenerator fileNameGenerator;

	private volatile Boolean deleteSourceFiles;

	private volatile Boolean autoCreateDirectory;

	private volatile Boolean requiresReply;

	private volatile Long sendTimeout;

	private volatile Integer order;
	
	private volatile String temporaryFileSuffix;

	private final Object initializationMonitor = new Object();


	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void setDirectory(File directory) {
		this.directory = directory;
	}
	
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setChannelResolver(ChannelResolver channelResolver) {
		this.channelResolver = channelResolver;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setDeleteSourceFiles(Boolean deleteSourceFiles) {
		this.deleteSourceFiles = deleteSourceFiles;
	}

	public void setAutoCreateDirectory(Boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}

	public void setRequiresReply(Boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	public void setSendTimeout(Long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setOrder(Integer order) {
		this.order = order;
	}

	public FileWritingMessageHandler getObject() throws Exception {
		if (this.handler == null) {
			initHandler();
		}
		return this.handler;
	}

	public Class<?> getObjectType() {
		return FileWritingMessageHandler.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initHandler() {
		synchronized (this.initializationMonitor) {
			if (this.handler != null) {
				return;
			}
			this.handler = new FileWritingMessageHandler(this.directory);
			if (this.outputChannel != null) {
				this.handler.setOutputChannel(this.outputChannel);
			}
			if (this.channelResolver != null) {
				this.handler.setChannelResolver(this.channelResolver);
			}
			if (this.charset != null) {
				this.handler.setCharset(this.charset);
			}
			if (this.fileNameGenerator != null) {
				this.handler.setFileNameGenerator(this.fileNameGenerator);
			}
			if (this.deleteSourceFiles != null) {
				this.handler.setDeleteSourceFiles(this.deleteSourceFiles);
			}
			if (this.autoCreateDirectory != null) {
				this.handler.setAutoCreateDirectory(this.autoCreateDirectory);
			}
			if (this.requiresReply != null) {
				this.handler.setRequiresReply(this.requiresReply);
			}
			if (this.sendTimeout != null) {
				this.handler.setSendTimeout(this.sendTimeout);
			}
			if (this.order != null) {
				this.handler.setOrder(this.order);
			}
			this.handler.setTemporaryFileSuffix(this.temporaryFileSuffix);
			this.handler.setBeanFactory(this.beanFactory);
			this.handler.afterPropertiesSet();
		}
	}

}
