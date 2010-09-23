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
package org.springframework.integration.sftp.impl;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.entries.CompositeEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;
import org.springframework.integration.sftp.QueuedSftpSessionPool;
import org.springframework.integration.sftp.SftpEntryNamer;
import org.springframework.integration.sftp.SftpSessionFactory;
import org.springframework.integration.sftp.config.SftpSessionUtils;
import org.springframework.util.StringUtils;

import java.io.File;


/**
 * a factory bean to hide the fairly complex configuration possibilities for an SFTP endpoint
 *
 * @author Josh Long
 */
public class SftpRemoteFileSystemSynchronizingMessageSourceFactoryBean extends AbstractFactoryBean<SftpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {
	/**
	 * injected by the container
	 */
	private volatile ResourceLoader resourceLoader;
	private volatile Resource localDirectoryResource;
	private volatile String localDirectoryPath;
	private volatile String autoCreateDirectories;
	private volatile String autoDeleteRemoteFilesOnSync;
	private volatile String filenamePattern;
	private volatile EntryListFilter<ChannelSftp.LsEntry> filter;
	private int port = 22;
	private String host;
	private String keyFile;
	private String keyFilePassword;
	private String password;
	private String remoteDirectory;
	private String username;

	@SuppressWarnings("unused")
	public void setLocalDirectoryResource(Resource localDirectoryResource) {
		this.localDirectoryResource = localDirectoryResource;
	}

	@SuppressWarnings("unused")
	public void setLocalDirectoryPath(String localDirectoryPath) {
		this.localDirectoryPath = localDirectoryPath;
	}

	@SuppressWarnings("unused")
	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	@SuppressWarnings("unused")
	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
	}

	@SuppressWarnings("unused")
	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	@SuppressWarnings("unused")
	public void setFilter(EntryListFilter<ChannelSftp.LsEntry> filter) {
		this.filter = filter;
	}

	@SuppressWarnings("unused")
	public void setPort(int port) {
		this.port = port;
	}

	@SuppressWarnings("unused")
	public void setHost(String host) {
		this.host = host;
	}

	@SuppressWarnings("unused")
	public void setKeyFile(String keyFile) {
		this.keyFile = keyFile;
	}

	@SuppressWarnings("unused")
	public void setKeyFilePassword(String keyFilePassword) {
		this.keyFilePassword = keyFilePassword;
	}

	@SuppressWarnings("unused")
	public void setPassword(String password) {
		this.password = password;
	}

	@SuppressWarnings("unused")
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@SuppressWarnings("unused")
	public void setUsername(String username) {
		this.username = username;
	}

	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}

	@Override
	public Class<?> getObjectType() {
		return SftpRemoteFileSystemSynchronizingMessageSourceFactoryBean.class;
	}

	@Override
	protected SftpInboundRemoteFileSystemSynchronizingMessageSource createInstance()
			throws Exception {
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

		this.localDirectoryResource = this.fromText(localDirectoryPath);

		// remote predicates
		SftpEntryNamer sftpEntryNamer = new SftpEntryNamer();
		CompositeEntryListFilter<ChannelSftp.LsEntry> compositeFtpFileListFilter = new CompositeEntryListFilter<ChannelSftp.LsEntry>();

		if (StringUtils.hasText(this.filenamePattern)) {
			PatternMatchingEntryListFilter<ChannelSftp.LsEntry> ftpFilePatternMatchingEntryListFilter = new PatternMatchingEntryListFilter<ChannelSftp.LsEntry>(sftpEntryNamer, filenamePattern);
			compositeFtpFileListFilter.addFilter(ftpFilePatternMatchingEntryListFilter);
		}

		if (this.filter != null) {
			compositeFtpFileListFilter.addFilter(this.filter);
		}

		this.filter = compositeFtpFileListFilter;

		// pools
		SftpSessionFactory sessionFactory = SftpSessionUtils.buildSftpSessionFactory(this.host, this.password, this.username, this.keyFile, this.keyFilePassword, this.port);

		QueuedSftpSessionPool pool = new QueuedSftpSessionPool(15, sessionFactory);
		pool.afterPropertiesSet();

		SftpInboundRemoteFileSystemSynchronizer sftpSync = new SftpInboundRemoteFileSystemSynchronizer();
		sftpSync.setClientPool(pool);
		sftpSync.setLocalDirectory(this.localDirectoryResource);
		sftpSync.setShouldDeleteSourceFile(ackRemoteDir);
		sftpSync.setFilter(compositeFtpFileListFilter);
		sftpSync.setBeanFactory(this.getBeanFactory());
		sftpSync.setRemotePath(this.remoteDirectory);
		sftpSync.afterPropertiesSet(); // todo is this correct  ?
		sftpSync.start(); //todo

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

	private Resource fromText(String path) {
		ResourceEditor resourceEditor = new ResourceEditor(this.resourceLoader);
		resourceEditor.setAsText(path);

		return (Resource) resourceEditor.getValue();
	}
}
