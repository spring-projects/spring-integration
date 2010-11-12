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

import java.io.File;

import org.apache.commons.lang.SystemUtils;

import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.sftp.filters.SftpPatternMatchingFileListFilter;
import org.springframework.integration.sftp.inbound.SftpInboundRemoteFileSystemSynchronizer;
import org.springframework.integration.sftp.inbound.SftpInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.sftp.session.QueuedSftpSessionPool;
import org.springframework.integration.sftp.session.SftpSessionFactoryBean;
import org.springframework.integration.sftp.session.SftpSessionUtils;
import org.springframework.util.StringUtils;

import com.jcraft.jsch.ChannelSftp;

/**
 * Factory bean to hide the fairly complex configuration possibilities for an SFTP endpoint
 *
 * @author Josh Long
 */
class SftpInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean
		extends AbstractFactoryBean<SftpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {

	private volatile ResourceLoader resourceLoader;

	private volatile Resource localDirectoryResource;

	private volatile String localDirectoryPath;

	private volatile String autoCreateDirectories;

	private volatile String autoDeleteRemoteFilesOnSync;

	private volatile String filenamePattern;

	private volatile FileListFilter<ChannelSftp.LsEntry> filter;

	private int port = 22;

	private String host;

	private String keyFile;

	private String keyFilePassword;

	private String remoteDirectory;

	private String username;

	private String password;


	public void setLocalDirectoryResource(Resource localDirectoryResource) {
		this.localDirectoryResource = localDirectoryResource;
	}

	public void setLocalDirectoryPath(String localDirectoryPath) {
		this.localDirectoryPath = localDirectoryPath;
	}

	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
	}

	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	public void setFilter(FileListFilter<ChannelSftp.LsEntry> filter) {
		this.filter = filter;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	public void setKeyFilePassword(String keyFilePassword) {
		this.keyFilePassword = keyFilePassword;
	}

	/**
	 * Set the remote directory to synchronize with
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	/**
	 * Set the user name to be used for authentication with the remote server
	 */
	public void setUsername(String username) {
		this.username = username;
	}

	/**
	 * Set the password to be used for authentication with the remote server
	 * @param password
	 */
	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * {@inheritDoc}
	 */
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Class<?> getObjectType() {
		return SftpInboundRemoteFileSystemSynchronizingMessageSourceFactoryBean.class;
	}

	/**
	 * {@inheritDoc}
	 * @return Fully configured SftpInboundRemoteFileSystemSynchronizingMessageSource
	 */
	@Override
	protected SftpInboundRemoteFileSystemSynchronizingMessageSource createInstance() throws Exception {
		boolean autoCreatDirs = Boolean.parseBoolean(this.autoCreateDirectories);
		boolean ackRemoteDir = Boolean.parseBoolean(this.autoDeleteRemoteFilesOnSync);
		SftpInboundRemoteFileSystemSynchronizingMessageSource sftpMsgSrc = new SftpInboundRemoteFileSystemSynchronizingMessageSource();
		sftpMsgSrc.setAutoCreateDirectories(autoCreatDirs);

		// local directories
		if ((this.localDirectoryResource == null) && !StringUtils.hasText(this.localDirectoryPath)) {
			File tmp = SystemUtils.getJavaIoTmpDir();
			File sftpTmp = new File(tmp, "sftpInbound");
			this.localDirectoryPath = "file://" + sftpTmp.getAbsolutePath();
		}
		this.localDirectoryResource = this.resourceFromString(localDirectoryPath);

		// remote predicates
		CompositeFileListFilter<ChannelSftp.LsEntry> compositeFtpFileListFilter = new CompositeFileListFilter<ChannelSftp.LsEntry>();
		if (StringUtils.hasText(this.filenamePattern)) {
			SftpPatternMatchingFileListFilter sftpFilePatternMatchingEntryListFilter =
					new SftpPatternMatchingFileListFilter(filenamePattern);
			compositeFtpFileListFilter.addFilter(sftpFilePatternMatchingEntryListFilter);
		}
		if (this.filter != null) {
			compositeFtpFileListFilter.addFilter(this.filter);
		}
		this.filter = compositeFtpFileListFilter;

		// pools
		SftpSessionFactoryBean sessionFactory = SftpSessionUtils.buildSftpSessionFactory(this.host, this.password, this.username, this.keyFile, this.keyFilePassword, this.port);
		QueuedSftpSessionPool pool = new QueuedSftpSessionPool(15, sessionFactory);
		pool.afterPropertiesSet();

		SftpInboundRemoteFileSystemSynchronizer sftpSync = new SftpInboundRemoteFileSystemSynchronizer();
		sftpSync.setClientPool(pool);
		sftpSync.setLocalDirectory(this.localDirectoryResource);
		sftpSync.setShouldDeleteSourceFile(ackRemoteDir);
		sftpSync.setFilter(compositeFtpFileListFilter);
		sftpSync.setBeanFactory(this.getBeanFactory());
		sftpSync.setRemotePath(this.remoteDirectory);
		sftpSync.afterPropertiesSet();
		sftpSync.start();

		sftpMsgSrc.setRemotePredicate(compositeFtpFileListFilter);
		sftpMsgSrc.setSynchronizer(sftpSync);
		sftpMsgSrc.setClientPool(pool);
		sftpMsgSrc.setRemotePath(this.remoteDirectory);
		sftpMsgSrc.setLocalDirectory(this.localDirectoryResource);
		sftpMsgSrc.setBeanFactory(this.getBeanFactory());
		sftpMsgSrc.setAutoStartup(true);
		sftpMsgSrc.afterPropertiesSet();
		sftpMsgSrc.start();
		return sftpMsgSrc;
	}

	private Resource resourceFromString(String path) {
		ResourceEditor resourceEditor = new ResourceEditor(this.resourceLoader);
		resourceEditor.setAsText(path);
		return (Resource) resourceEditor.getValue();
	}

}
