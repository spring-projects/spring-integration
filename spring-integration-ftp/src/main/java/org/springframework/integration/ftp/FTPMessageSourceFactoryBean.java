package org.springframework.integration.ftp;

import org.apache.commons.lang.SystemUtils;
import org.apache.commons.net.ftp.FTPClient;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Map;
import java.util.regex.Pattern;


/**
 * Makes it easier to assemble the moving pieces involved in standing up an {@link org.springframework.integration.ftp.FTPMessageSourceFactoryBean}
 *
 * @author Josh Long
 */
public class FTPMessageSourceFactoryBean extends AbstractFactoryBean<FTPFileSource> implements ResourceLoaderAware, ApplicationContextAware {
    private int port;
    private boolean autoCreateDirectories;
    private String filenamePattern;
    private String username;
    private String password;
    private String host;
    private String remoteDirectory;
    private String localWorkingDirectory;
    private ApplicationContext applicationContext;
    private Resource localDirectoryResource;
    private FTPInboundSynchronizer ftpInboundSynchronizer;
    private TaskScheduler taskScheduler;
    private ResourceLoader resourceLoader;
    private FileReadingMessageSource fileReadingMessageSource;
    private int clientMode = FTPClient.ACTIVE_LOCAL_DATA_CONNECTION_MODE;

    /**
     * Used to teach the FTP adapter what files you are interested in receiving
     */
    private FTPFileListFilter filter;

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    public void setApplicationContext(ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setAutoCreateDirectories(boolean autoCreateDirectories) {
        this.autoCreateDirectories = autoCreateDirectories;
    }

    public void setRemoteDirectory(String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setLocalWorkingDirectory(String localWorkingDirectory) {
        this.localWorkingDirectory = localWorkingDirectory;
    }

    public void setResourceLoader(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public Class<?extends FTPFileSource> getObjectType() {
        return FTPFileSource.class;
    }

    public void setClientMode(int clientMode) {
        this.clientMode = clientMode;
    }

    public void setFilter(FTPFileListFilter filter) {
        this.filter = filter;
    }

    @Override
    protected FTPFileSource createInstance() throws Exception {
        // setup local dir
        if (!StringUtils.hasText(this.localWorkingDirectory)) {
            File tmp = SystemUtils.getJavaIoTmpDir();
            File ftpTmp = new File(tmp, "ftpInbound");
            this.localWorkingDirectory = "file://" + ftpTmp.getAbsolutePath();
        }

        Assert.hasText(this.localWorkingDirectory, "the local working directory can't be null!");

        ResourceEditor resourceEditor = new ResourceEditor(this.resourceLoader);
        resourceEditor.setAsText(this.localWorkingDirectory);
        this.localDirectoryResource = (Resource) resourceEditor.getValue();
        fileReadingMessageSource = new FileReadingMessageSource();

        this.ftpInboundSynchronizer = new FTPInboundSynchronizer();

        CompositeFTPFileListFilter compositeFTPFileListFilter = new CompositeFTPFileListFilter();

        if (StringUtils.hasText(this.filenamePattern)) {
            PatternMatchingFTPFileListFilter patternMatchingFTPFileListFilter = new PatternMatchingFTPFileListFilter();
            patternMatchingFTPFileListFilter.setPattern(Pattern.compile(this.filenamePattern));
            compositeFTPFileListFilter.addFilter(patternMatchingFTPFileListFilter);
        }

        if (this.filter != null) {
            compositeFTPFileListFilter.addFilter(this.filter);
        }

        this.ftpInboundSynchronizer.setFilter(compositeFTPFileListFilter);

        if (this.taskScheduler == null) {
            Map<String, TaskScheduler> tss = null;

            if ((tss = applicationContext.getBeansOfType(TaskScheduler.class)).keySet().size() != 0) {
                taskScheduler = tss.get(tss.keySet().iterator().next());
            }
        }

        if (null == taskScheduler) {
            ThreadPoolTaskScheduler threadPoolTaskScheduler = new ThreadPoolTaskScheduler();
            threadPoolTaskScheduler.setPoolSize(10);
            threadPoolTaskScheduler.setErrorHandler(new ErrorHandler() {
                    public void handleError(Throwable t) {
                        logger.debug("Error! ", t);
                    }
                });
            threadPoolTaskScheduler.setWaitForTasksToCompleteOnShutdown(true);
            threadPoolTaskScheduler.initialize();
            this.taskScheduler = threadPoolTaskScheduler;
        }

        DefaultFTPClientFactory defaultFTPClientFactory = new DefaultFTPClientFactory();
        defaultFTPClientFactory.setHost(this.host);
        defaultFTPClientFactory.setPassword(this.password);
        defaultFTPClientFactory.setPort(this.port);
        defaultFTPClientFactory.setRemoteWorkingDirectory(this.remoteDirectory);
        defaultFTPClientFactory.setUsername(this.username);
        defaultFTPClientFactory.setClientMode(this.clientMode);

        QueuedFTPClientPool queuedFTPClientPool = new QueuedFTPClientPool(15, defaultFTPClientFactory);

        this.ftpInboundSynchronizer.setClientPool(queuedFTPClientPool);
        this.ftpInboundSynchronizer.setLocalDirectory(this.localDirectoryResource);
        this.ftpInboundSynchronizer.setTaskScheduler(this.taskScheduler);
        assert this.localDirectoryResource != null : "the 'localDirectoryResource' can't be null at this point";

        if (this.autoCreateDirectories) {
            if (!this.localDirectoryResource.exists()) {
                try {
                    if (!localDirectoryResource.getFile().mkdirs()) {
                        logger.debug("attempted to ensure the existence of the local directory '" + this.localDirectoryResource.getFile().getAbsolutePath() + "' but didn't succeed");
                    }
                } catch (Throwable th) {
                    logger.debug("attempted to ensure the existence of the local directory '" + this.localDirectoryResource.getFile().getAbsolutePath() + "' but didn't succeed");
                }
            }
        }

        this.ftpInboundSynchronizer.afterPropertiesSet();

        FTPFileSource ftpFileSource = new FTPFileSource(this.fileReadingMessageSource, this.ftpInboundSynchronizer);
        ftpFileSource.setClientPool(queuedFTPClientPool);
        ftpFileSource.setLocalWorkingDirectory(this.localDirectoryResource);
        ftpFileSource.setSynchronizer(this.ftpInboundSynchronizer);
        ftpFileSource.setTaskScheduler(this.taskScheduler);
        ftpFileSource.setFileSource(this.fileReadingMessageSource);
        ftpFileSource.afterPropertiesSet();
        ftpFileSource.start();

        return ftpFileSource;
    }
}
