/*
 * Copyright 2002-2011 the original author or authors.
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
package org.springframework.integration.file.remote.handler;

import java.io.File;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.integration.file.DefaultFileNameGenerator;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;

/**
 * Factory bean that creates an instance of {@link FileTransferringMessageHandler}
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public class FileTransferringMessageHandlerFactoryBean<T> implements FactoryBean<FileTransferringMessageHandler<T>>, InitializingBean {
	
	private final Log logger = LogFactory.getLog(this.getClass());
	
	private volatile String temporaryFileSuffix =".writing";

	private volatile SessionFactory<T> sessionFactory;
	
	private volatile boolean cacheSessions;

	private volatile boolean autoCreateDirectory = false;

	private volatile Expression remoteDirectoryExpression;
	
	private volatile Expression temporaryRemoteDirectoryExpression;

	private volatile FileNameGenerator fileNameGenerator = new DefaultFileNameGenerator();

	private volatile File temporaryDirectory = new File(System.getProperty("java.io.tmpdir"));

	private volatile String charset = "UTF-8";

	private volatile String remoteFileSeparator = "/";
	
	private volatile Integer order;
	
	private volatile FileTransferringMessageHandler<T> handler;

	public FileTransferringMessageHandler<T> getObject() throws Exception {
		if (this.handler == null){
			this.initializeHandler();
		}
		return this.handler;
	}
	
	public void setCacheSessions(boolean cacheSessions) {
		logger.warn("The 'cache-sessions' attribute is deprecated since v2.1. Consider configuring CachingSessionFactory explicitly");	
		this.cacheSessions = cacheSessions;
	}
	
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		Assert.hasText(temporaryFileSuffix, "'temporaryFileSuffix' must not be empty");
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	public void setSessionFactory(SessionFactory<T> sessionFactory) {
		Assert.notNull(sessionFactory, "'sessionFactory' must not be null");
		this.sessionFactory = sessionFactory;
	}

	public void setAutoCreateDirectory(boolean autoCreateDirectory) {
		this.autoCreateDirectory = autoCreateDirectory;
	}
	
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "'remoteDirectoryExpression' must not be null");
		this.remoteDirectoryExpression = remoteDirectoryExpression;
	}
	
	public void setTemporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		Assert.notNull(temporaryRemoteDirectoryExpression, "'temporaryRemoteDirectoryExpression' must not be null");
		this.temporaryRemoteDirectoryExpression = temporaryRemoteDirectoryExpression;
	}

	public void setFileNameGenerator(FileNameGenerator fileNameGenerator) {
		Assert.notNull(fileNameGenerator, "'fileNameGenerator' must not be null");
		this.fileNameGenerator = fileNameGenerator;
	}

	public void setTemporaryDirectory(File temporaryDirectory) {
		Assert.notNull(temporaryDirectory, "'temporaryDirectory' must not be null");
		this.temporaryDirectory = temporaryDirectory;
	}

	public void setCharset(String charset) {
		Assert.hasText(charset, "'charset' must not be empty");
		this.charset = charset;
	}

	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}
	
	public void setOrder(Integer order) {
		Assert.notNull(order, "'order' must not be null");
		this.order = order;
	}

	public Class<?> getObjectType() {
		return FileTransferringMessageHandler.class;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {
		this.initializeHandler();
	}
	
	private void initializeHandler(){
		Assert.notNull(sessionFactory, "'sessionFactory' must not be null");
		if (this.cacheSessions && !(sessionFactory instanceof CachingSessionFactory)){
			this.sessionFactory = new CachingSessionFactory<T>(this.sessionFactory);
		}
		handler = new FileTransferringMessageHandler<T>(sessionFactory);
		handler.setTemporaryFileSuffix(this.temporaryFileSuffix);
		handler.setAutoCreateDirectory(this.autoCreateDirectory);
		if (this.remoteDirectoryExpression != null){
			handler.setRemoteDirectoryExpression(this.remoteDirectoryExpression);
		}
		if (this.temporaryRemoteDirectoryExpression != null){
			handler.setTemporaryRemoteDirectoryExpression(this.temporaryRemoteDirectoryExpression);
		}
		
		handler.setFileNameGenerator(this.fileNameGenerator);
		handler.setTemporaryDirectory(this.temporaryDirectory);
		handler.setCharset(this.charset);
		handler.setRemoteFileSeparator(this.remoteFileSeparator);
		if (this.order != null){
			handler.setOrder(this.order);
		}
		
	}

}
