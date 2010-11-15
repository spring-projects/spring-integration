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
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
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
class FtpInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean
		extends AbstractFactoryBean<FtpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {

	private volatile String autoCreateDirectories;

	private volatile String filenamePattern;

	volatile String host;

	volatile String port;

	volatile String username;

	volatile String password;

	volatile String remoteDirectory;

	volatile int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

	volatile int fileType = FTP.BINARY_FILE_TYPE;

	volatile String defaultFtpInboundFolderName = "ftpInbound";

	private volatile String localWorkingDirectory;

	private volatile Resource localDirectoryResource;

	private volatile ResourceLoader resourceLoader;

	private volatile FileListFilter<FTPFile> filter;

	private volatile String autoDeleteRemoteFilesOnSync;


	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
	}

	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public void setLocalWorkingDirectory(String localWorkingDirectory) {
		this.localWorkingDirectory = localWorkingDirectory;
	}

	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
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

	protected AbstractFtpClientFactory<?> initializeFactory(AbstractFtpClientFactory<?> factory) throws Exception {
		factory.setHost(this.host);
		if (StringUtils.hasText(this.port)) {
			factory.setPort(Integer.parseInt(this.port));
		}
		factory.setUsername(this.username);
		factory.setPassword(this.password);
		factory.setRemoteWorkingDirectory(this.remoteDirectory);
		factory.setClientMode(this.clientMode);
		factory.setFileType(this.fileType);
		return factory;
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
		AbstractFtpClientFactory<?> factory = this.createClientFactory();
		QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, this.initializeFactory(factory));
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
