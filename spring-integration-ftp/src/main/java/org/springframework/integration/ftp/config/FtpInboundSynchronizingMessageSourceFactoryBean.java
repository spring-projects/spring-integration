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

import java.io.File;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.ftp.client.AbstractFtpClientFactory;
import org.springframework.integration.ftp.client.DefaultFtpClientFactory;
import org.springframework.integration.ftp.client.QueuedFtpClientPool;
import org.springframework.integration.ftp.filters.FtpPatternMatchingFileListFilter;
import org.springframework.integration.ftp.inbound.FtpInboundRemoteFileSystemSynchronizer;
import org.springframework.integration.ftp.inbound.FtpInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.util.StringUtils;

/**
 * Factory to make building the namespace easier
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class FtpInboundSynchronizingMessageSourceFactoryBean
		extends AbstractFactoryBean<FtpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {

	private volatile String autoCreateDirectories;

	private volatile String filenamePattern;
	
	private volatile AbstractFtpClientFactory<?> clientFactory;

	volatile String defaultFtpInboundFolderName = "ftpInbound";

	private volatile String localWorkingDirectory;

	private volatile Resource localDirectoryResource;

	private volatile ResourceLoader resourceLoader;

	private volatile FileListFilter<FTPFile> filter;

	private volatile String autoDeleteRemoteFilesOnSync;

	public void setClientFactory(AbstractFtpClientFactory<?> clientFactory) {
		this.clientFactory = clientFactory;
	}

	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
	}

	public void setLocalWorkingDirectory(String localWorkingDirectory) {
		this.localWorkingDirectory = localWorkingDirectory;
	}

	public void setFilter(FileListFilter<FTPFile> filter) {
		this.filter = filter;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Class<?> getObjectType() {
		return FtpInboundRemoteFileSystemSynchronizingMessageSource.class;
	}

	private Resource resolveResource(String path) {
		ResourceEditor resourceEditor = new ResourceEditor(this.resourceLoader);
		resourceEditor.setAsText(path);
		return (Resource) resourceEditor.getValue();
	}

	@Override
	protected FtpInboundRemoteFileSystemSynchronizingMessageSource createInstance() throws Exception {
		boolean autoCreatDirs = Boolean.parseBoolean(this.autoCreateDirectories);
		boolean ackRemoteDir = Boolean.parseBoolean(this.autoDeleteRemoteFilesOnSync);
		FtpInboundRemoteFileSystemSynchronizingMessageSource messageSource =
				new FtpInboundRemoteFileSystemSynchronizingMessageSource();
		messageSource.setAutoCreateDirectories(autoCreatDirs);
		if (!StringUtils.hasText(this.localWorkingDirectory)) {
			File tmp = new File(SystemUtils.getJavaIoTmpDir(), this.defaultFtpInboundFolderName);
			this.localWorkingDirectory = "file://" + tmp.getAbsolutePath();
		}
		this.localDirectoryResource = this.resolveResource(this.localWorkingDirectory);
		CompositeFileListFilter<FTPFile> compositeFilter = new CompositeFileListFilter<FTPFile>();
		if (StringUtils.hasText(this.filenamePattern)) {
			FtpPatternMatchingFileListFilter ftpFilePatternMatchingFileListFilter =
					new FtpPatternMatchingFileListFilter(this.filenamePattern);
			compositeFilter.addFilter(ftpFilePatternMatchingFileListFilter);
		}
		if (this.filter != null) {
			compositeFilter.addFilter(this.filter);
		}
		QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, this.clientFactory);
		FtpInboundRemoteFileSystemSynchronizer synchronizer = new FtpInboundRemoteFileSystemSynchronizer();
		synchronizer.setClientPool(queuedFtpClientPool);
		synchronizer.setLocalDirectory(this.localDirectoryResource);
		synchronizer.setShouldDeleteSourceFile(ackRemoteDir);
		synchronizer.setFilter(compositeFilter);
		messageSource.setRemotePredicate(compositeFilter);
		messageSource.setSynchronizer(synchronizer);
		messageSource.setClientPool(queuedFtpClientPool);
		messageSource.setLocalDirectory(this.localDirectoryResource);
		messageSource.setBeanFactory(this.getBeanFactory());
		messageSource.setAutoStartup(true);
		messageSource.afterPropertiesSet();
		messageSource.start();
		return messageSource;
	}
	
	protected AbstractFtpClientFactory<?> createClientFactory(){
		return new DefaultFtpClientFactory();
	}

}
