package org.springframework.integration.ftp.impl;

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
import org.springframework.integration.ftp.*;
import org.springframework.util.StringUtils;

import java.io.File;


/**
 * Factory to make building the namespace easier
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean extends AbstractFactoryBean<FtpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {
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

	public void setFileType(int fileType) {
		this.fileType = fileType;
	}

	private volatile String autoDeleteRemoteFilesOnSync;

	@SuppressWarnings("unused")
	public void setAutoDeleteRemoteFilesOnSync(String autoDeleteRemoteFilesOnSync) {
		this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
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

	protected AbstractFtpClientFactory defaultClientFactory() throws Exception {
		 return ClientFactorySupport.ftpClientFactory( this.host ,  Integer.parseInt(this.port) , this.remoteDirectory , this.username ,this.password, this.clientMode );
	}

	protected String defaultFtpInboundFolderName = "ftpInbound";

	@Override
	protected FtpInboundRemoteFileSystemSynchronizingMessageSource createInstance()
			throws Exception {
		boolean autoCreatDirs = Boolean.parseBoolean(this.autoCreateDirectories);
		boolean ackRemoteDir = Boolean.parseBoolean(this.autoDeleteRemoteFilesOnSync);

		FtpInboundRemoteFileSystemSynchronizingMessageSource ftpRemoteFileSystemSynchronizingMessageSource = new FtpInboundRemoteFileSystemSynchronizingMessageSource();
		ftpRemoteFileSystemSynchronizingMessageSource.setAutoCreateDirectories(autoCreatDirs);

		if (!StringUtils.hasText(this.localWorkingDirectory)) {
			File tmp = new File(SystemUtils.getJavaIoTmpDir(), defaultFtpInboundFolderName);
			this.localWorkingDirectory = "file://" + tmp.getAbsolutePath();
		}

		this.localDirectoryResource = this.fromText(this.localWorkingDirectory);

		FtpFileEntryNamer ftpFileEntryNamer = new FtpFileEntryNamer();
		CompositeEntryListFilter<FTPFile> compositeFtpFileListFilter = new CompositeEntryListFilter<FTPFile>();

		if (StringUtils.hasText(this.filenamePattern)) {
			PatternMatchingEntryListFilter<FTPFile> ftpFilePatternMatchingEntryListFilter = new PatternMatchingEntryListFilter<FTPFile>(ftpFileEntryNamer, filenamePattern);
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

	@SuppressWarnings("unused")
	public void setPort(String port) {
		this.port = port;
	}

	@SuppressWarnings("unused")
	public void setAutoCreateDirectories(String autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	@SuppressWarnings("unused")
	public void setFilenamePattern(String filenamePattern) {
		this.filenamePattern = filenamePattern;
	}

	@SuppressWarnings("unused")
	public void setUsername(String username) {
		this.username = username;
	}

	@SuppressWarnings("unused")
	public void setPassword(String password) {
		this.password = password;
	}

	@SuppressWarnings("unused")
	public void setHost(String host) {
		this.host = host;
	}

	@SuppressWarnings("unused")
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@SuppressWarnings("unused")
	public void setLocalWorkingDirectory(String localWorkingDirectory) {
		this.localWorkingDirectory = localWorkingDirectory;
	}

	@SuppressWarnings("unused")
	public void setFilter(EntryListFilter<FTPFile> filter) {
		this.filter = filter;
	}

	@SuppressWarnings("unused")
	public void setClientMode(int clientMode) {
		this.clientMode = clientMode;
	}

	@SuppressWarnings("unused")
	public void setResourceLoader(ResourceLoader resourceLoader) {
		this.resourceLoader = resourceLoader;
	}
}
