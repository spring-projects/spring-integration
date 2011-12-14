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
package org.springframework.integration.file.remote.synchronizer;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.CachingSessionFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Factory bean that serves as a base class for other factory beans that 
 * create instances of {@link AbstractInboundFileSynchronizer}
 * 
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public abstract class AbstractInboundFileSynchronizerFactoryBean<T> implements FactoryBean<AbstractInboundFileSynchronizer<T>>, InitializingBean {

	private volatile String remoteFileSeparator = "/";

	private volatile String temporaryFileSuffix =".writing";

	private volatile Expression localFilenameGeneratorExpression;

	private volatile String remoteDirectory;

	private volatile SessionFactory<T> sessionFactory;

	private volatile FileListFilter<T> filter;

	private volatile boolean deleteRemoteFiles;
	
	private volatile boolean cacheSessions;

	private volatile AbstractInboundFileSynchronizer<T> synchronizer;
	
	public AbstractInboundFileSynchronizer<T> getObject() throws Exception {
		if (this.synchronizer == null){
			this.initializeSynchronizer();
		}
		return this.synchronizer;
	}
	
	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	
	public void setCacheSessions(boolean cacheSessions) {
		this.cacheSessions = cacheSessions;
	}
	
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		Assert.hasText(temporaryFileSuffix, "'temporaryFileSuffix' must not be empty");
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		Assert.hasText(remoteDirectory, "'remoteDirectory' must not be empty");
		this.remoteDirectory = remoteDirectory;
	}

	public void setSessionFactory(SessionFactory<T> sessionFactory) {
		Assert.notNull(sessionFactory, "'sessionFactory' must not be null");
		this.sessionFactory = sessionFactory;
	}

	public void setFilter(FileListFilter<T> filter) {
		this.filter = filter;
	}

	public boolean isSingleton() {
		return true;
	}

	public void afterPropertiesSet() throws Exception {
		this.initializeSynchronizer();
	}

	private void initializeSynchronizer(){
		Assert.notNull(sessionFactory, "'sessionFactory' must not be null");
		if (this.cacheSessions && !(sessionFactory instanceof CachingSessionFactory)){
			this.sessionFactory = new CachingSessionFactory<T>(this.sessionFactory);
		}
		this.synchronizer = this.getInstance(sessionFactory);
		this.synchronizer.setRemoteFileSeparator(this.remoteFileSeparator);
		this.synchronizer.setTemporaryFileSuffix(this.temporaryFileSuffix);
		if (this.localFilenameGeneratorExpression != null){
			this.synchronizer.setLocalFilenameGeneratorExpression(this.localFilenameGeneratorExpression);
		}
		if (StringUtils.hasText(remoteDirectory)){
			this.synchronizer.setRemoteDirectory(this.remoteDirectory);
		}
		if (this.filter != null){
			this.synchronizer.setFilter(this.filter);
		}
		this.synchronizer.setDeleteRemoteFiles(deleteRemoteFiles);
	}
	
	protected abstract AbstractInboundFileSynchronizer<T> getInstance(SessionFactory<T> sessionFactory);
}
