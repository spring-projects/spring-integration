package org.springframework.integration.ftp.impl;

import org.apache.commons.lang.SystemUtils;
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
import org.springframework.integration.ftp.DefaultFtpClientFactory;
import org.springframework.integration.ftp.FtpFileEntryNamer;
import org.springframework.integration.ftp.QueuedFtpClientPool;
import org.springframework.util.StringUtils;

import java.io.File;


/**
 * Factory to make building the namespace easier
 *
 * @author Josh Long
 */
public class FtpRemoteFileSystemSynchronizingMessageSourceFactoryBean extends AbstractFactoryBean<FtpInboundRemoteFileSystemSynchronizingMessageSource> implements ResourceLoaderAware {
    private volatile String port;
    private volatile String autoCreateDirectories;
    private volatile String filenamePattern;
    private volatile String username;
    private volatile String password;
    private volatile String host;
    private volatile String remoteDirectory;
    private volatile String localWorkingDirectory;
    private volatile ResourceLoader resourceLoader;
    private volatile Resource localDirectoryResource;
    private volatile EntryListFilter<FTPFile> filter;
    private volatile int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;
    private volatile String autoDeleteRemoteFilesOnSync;

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

    private DefaultFtpClientFactory defaultFtpClientFactory() {
        DefaultFtpClientFactory defaultFtpClientFactory = new DefaultFtpClientFactory();
        defaultFtpClientFactory.setHost(this.host);
        defaultFtpClientFactory.setPassword(this.password);
        defaultFtpClientFactory.setPort(Integer.parseInt(this.port));
        defaultFtpClientFactory.setRemoteWorkingDirectory(this.remoteDirectory);
        defaultFtpClientFactory.setUsername(this.username);
        defaultFtpClientFactory.setClientMode(this.clientMode);

        return defaultFtpClientFactory;
    }

    @Override
    protected FtpInboundRemoteFileSystemSynchronizingMessageSource createInstance()
            throws Exception {
        boolean autoCreatDirs = Boolean.parseBoolean(this.autoCreateDirectories);
        boolean ackRemoteDir = Boolean.parseBoolean(this.autoDeleteRemoteFilesOnSync);

        FtpInboundRemoteFileSystemSynchronizingMessageSource ftpRemoteFileSystemSynchronizingMessageSource = new FtpInboundRemoteFileSystemSynchronizingMessageSource();
        ftpRemoteFileSystemSynchronizingMessageSource.setAutoCreateDirectories(autoCreatDirs);

        if (!StringUtils.hasText(this.localWorkingDirectory)) {
            File tmp = new File(SystemUtils.getJavaIoTmpDir(), "ftpInbound");
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

        QueuedFtpClientPool queuedFtpClientPool = new QueuedFtpClientPool(15, defaultFtpClientFactory());

        FtpInboundRemoteFileSystemSynchronizer ftpRemoteFileSystemSynchronizer = new FtpInboundRemoteFileSystemSynchronizer();
        ftpRemoteFileSystemSynchronizer.setClientPool(queuedFtpClientPool);
        ftpRemoteFileSystemSynchronizer.setLocalDirectory(this.localDirectoryResource);
        ftpRemoteFileSystemSynchronizer.setShouldDeleteSourceFile(ackRemoteDir);

        if (compositeFtpFileListFilter != null) {
            ftpRemoteFileSystemSynchronizer.setFilter(compositeFtpFileListFilter);
            ftpRemoteFileSystemSynchronizingMessageSource.setRemotePredicate(compositeFtpFileListFilter);
        }

        ftpRemoteFileSystemSynchronizingMessageSource.setSynchronizer(ftpRemoteFileSystemSynchronizer);
        ftpRemoteFileSystemSynchronizingMessageSource.setClientPool(queuedFtpClientPool);

        ftpRemoteFileSystemSynchronizingMessageSource.setLocalDirectory(this.localDirectoryResource);
        ftpRemoteFileSystemSynchronizingMessageSource.setBeanFactory(this.getBeanFactory());
        ftpRemoteFileSystemSynchronizingMessageSource.setAutoStartup(true);
        ftpRemoteFileSystemSynchronizingMessageSource.afterPropertiesSet();
        ftpRemoteFileSystemSynchronizingMessageSource.start();

        return ftpRemoteFileSystemSynchronizingMessageSource;
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
}
