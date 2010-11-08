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

package org.springframework.integration.ftp;

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
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;
import org.springframework.util.StringUtils;

/**
 * Factory to make building the namespace easier
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean
		extends AbstractFactoryBean<FtpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {

	protected volatile String port;

	protected volatile String autoCreateDirectories;

	protected volatile String filenamePattern;

	protected volatile String username;

	protected volatile String password;

	protected volatile String host;

	protected volatile String remoteDirectory;

	protected volatile String localWorkingDirectory;

	protected volatile ResourceLoader resourceLoader;

	protected volatile Resource localDirectoryResource;

	protected volatile EntryListFilter<FTPFile> filter;

	protected volatile int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

	protected volatile int fileType = FTP.BINARY_FILE_TYPE;

	private volatile String autoDeleteRemoteFilesOnSync;

	protected String defaultFtpInboundFolderName = "ftpInbound";


	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
	}

	public void setPort(String port) {
		this.port = port;
	}

	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
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

	public void setLocalWorkingDirectory(String localWorkingDirectory) {
		this.localWorkingDirectory = localWorkingDirectory;
	}

	public void setFilter(EntryListFilter<FTPFile> filter) {
		this.filter = filter;
	}

	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Class<?> getObjectType() {
		return FtpInboundRemoteFileSystemSynchronizingMessageSource.class;
	}

	private Resource fromText(String path) {
		ResourceEditor resourceEditor = new ResourceEditor(this.resourceLoader);
		resourceEditor.setAsText(path);
		return (Resource) resourceEditor.getValue();
	}

	protected AbstractFtpClientFactory<?> defaultClientFactory() throws Exception {
		return ClientFactorySupport.ftpClientFactory(this.host,
				Integer.parseInt(this.port), this.remoteDirectory, this.username,
				this.password, this.clientMode, this.fileType);
	}

	@Override
	protected FtpInboundRemoteFileSystemSynchronizingMessageSource createInstance() throws Exception {
		boolean autoCreatDirs = Boolean.parseBoolean(this.autoCreateDirectories);
		boolean ackRemoteDir = Boolean.parseBoolean(this.autoDeleteRemoteFilesOnSync);
		FtpInboundRemoteFileSystemSynchronizingMessageSource ftpRemoteFileSystemSynchronizingMessageSource =
				new FtpInboundRemoteFileSystemSynchronizingMessageSource();
		ftpRemoteFileSystemSynchronizingMessageSource.setAutoCreateDirectories(autoCreatDirs);
		if (!StringUtils.hasText(this.localWorkingDirectory)) {
			File tmp = new File(SystemUtils.getJavaIoTmpDir(), defaultFtpInboundFolderName);
			this.localWorkingDirectory = "file://" + tmp.getAbsolutePath();
		}
		this.localDirectoryResource = this.fromText(this.localWorkingDirectory);
		FtpFileEntryNameExtractor fileEntryNameExtractor = new FtpFileEntryNameExtractor();
		CompositeEntryListFilter<FTPFile> compositeFtpFileListFilter = new CompositeEntryListFilter<FTPFile>();
		if (StringUtils.hasText(this.filenamePattern)) {
			PatternMatchingEntryListFilter<FTPFile> ftpFilePatternMatchingEntryListFilter =
					new PatternMatchingEntryListFilter<FTPFile>(fileEntryNameExtractor, filenamePattern);
			compositeFtpFileListFilter.addFilter(ftpFilePatternMatchingEntryListFilter);
		}
		if (this.filter != null) {
			compositeFtpFileListFilter.addFilter(this.filter);
		}
		QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, defaultClientFactory());
		FtpInboundRemoteFileSystemSynchronizer ftpRemoteFileSystemSynchronizer = new FtpInboundRemoteFileSystemSynchronizer();
		ftpRemoteFileSystemSynchronizer.setClientPool(queuedFtpClientPool);
		ftpRemoteFileSystemSynchronizer.setLocalDirectory(this.localDirectoryResource);
		ftpRemoteFileSystemSynchronizer.setShouldDeleteSourceFile(ackRemoteDir);
		ftpRemoteFileSystemSynchronizer.setFilter(compositeFtpFileListFilter);
		ftpRemoteFileSystemSynchronizingMessageSource.setRemotePredicate(compositeFtpFileListFilter);
		ftpRemoteFileSystemSynchronizingMessageSource.setSynchronizer(ftpRemoteFileSystemSynchronizer);
		ftpRemoteFileSystemSynchronizingMessageSource.setClientPool(queuedFtpClientPool);
		ftpRemoteFileSystemSynchronizingMessageSource.setLocalDirectory(this.localDirectoryResource);
		ftpRemoteFileSystemSynchronizingMessageSource.setBeanFactory(this.getBeanFactory());
		ftpRemoteFileSystemSynchronizingMessageSource.setAutoStartup(true);
		ftpRemoteFileSystemSynchronizingMessageSource.afterPropertiesSet();
		ftpRemoteFileSystemSynchronizingMessageSource.start();
		return ftpRemoteFileSystemSynchronizingMessageSource;
	}

}
