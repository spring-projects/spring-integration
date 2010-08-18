/*
 * Copyright 2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.sftp.config;

import org.apache.commons.lang.SystemUtils;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.AbstractFactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ResourceLoaderAware;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceEditor;
import org.springframework.core.io.ResourceLoader;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.sftp.*;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.ErrorHandler;
import org.springframework.util.StringUtils;

import java.io.File;
import java.util.Map;


/**
 * Building a {@link org.springframework.integration.sftp.SFTPMessageSource} is a complicated because we also
 * use a {@link org.springframework.integration.file.FileReadingMessageSource} to handle the "receipt" of files in
 * a {@link #localWorkingDirectory}.
 *
 * @author Josh Long
 */
public class SFTPMessageSourceFactoryBean extends AbstractFactoryBean<SFTPMessageSource> implements ApplicationContextAware, ResourceLoaderAware {
    private ApplicationContext applicationContext;
    private FileReadingMessageSource fileReadingMessageSource;
    private Resource localDirectoryResource;
    private ResourceLoader resourceLoader;
    private SFTPInboundSynchronizer synchronizer;
    private String host;
    private String keyFile;
    private String keyFilePassword;
    private String localWorkingDirectory;
    private String password;
    private String remoteDirectory;
    private String username;
    private TaskScheduler taskScheduler;
    private Trigger trigger;
    private boolean autoCreateDirectories;
    private boolean autoDeleteRemoteFilesOnSync;
    private int port = 22;
    private SFTPFileListFilter filter;
    private String filenamePattern;

    public FileReadingMessageSource getFileReadingMessageSource() {
        return fileReadingMessageSource;
    }

    public String getHost() {
        return host;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public String getKeyFilePassword() {
        return keyFilePassword;
    }

    public String getLocalWorkingDirectory() {
        return localWorkingDirectory;
    }

    @Override
    public Class<?extends SFTPMessageSource> getObjectType() {
        return SFTPMessageSource.class;
    }

    public String getPassword() {
        return password;
    }

    public int getPort() {
        return port;
    }

    public String getRemoteDirectory() {
        return remoteDirectory;
    }

    public SFTPInboundSynchronizer getSynchronizer() {
        return synchronizer;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    // this is the ultimate layer of control
    // users will configure theeir entire experience using this class and trust that a working
    // component comes out as a result of their input
    // we need to support user/pw/keys/host/port/auto-delete properties
    public String getUsername() {
        return username;
    }

    public boolean isAutoCreateDirectories() {
        return autoCreateDirectories;
    }

    public boolean isAutoDeleteRemoteFilesOnSync() {
        return autoDeleteRemoteFilesOnSync;
    }

    public void setApplicationContext(final ApplicationContext applicationContext)
        throws BeansException {
        this.applicationContext = applicationContext;
    }

    public void setAutoCreateDirectories(final boolean autoCreateDirectories) {
        this.autoCreateDirectories = autoCreateDirectories;
    }

    public void setAutoDeleteRemoteFilesOnSync(final boolean autoDeleteRemoteFilesOnSync) {
        this.autoDeleteRemoteFilesOnSync = autoDeleteRemoteFilesOnSync;
    }

    public void setFileReadingMessageSource(final FileReadingMessageSource fileReadingMessageSource) {
        this.fileReadingMessageSource = fileReadingMessageSource;
    }

    public void setHost(final String host) {
        this.host = host;
    }

    public void setKeyFile(final String keyFile) {
        this.keyFile = keyFile;
    }

    public void setKeyFilePassword(final String keyFilePassword) {
        this.keyFilePassword = keyFilePassword;
    }

    public void setLocalWorkingDirectory(final String lwd) {
        this.localWorkingDirectory = lwd;
    }

    public void setPassword(final String password) {
        this.password = password;
    }

    public void setPort(final int port) {
        this.port = port;
    }

    public void setRemoteDirectory(final String remoteDirectory) {
        this.remoteDirectory = remoteDirectory;
    }

    public void setResourceLoader(final ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setSynchronizer(final SFTPInboundSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public void setTaskScheduler(final TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void setTrigger(final Trigger trigger) {
        this.trigger = trigger;
    }

    public void setFilenamePattern(String filenamePattern) {
        this.filenamePattern = filenamePattern;
    }

    public void setFilter(SFTPFileListFilter filter) {
        this.filter = filter;
    }

    public void setUsername(final String username) {
        this.username = username;
    }

    @Override
    protected SFTPMessageSource createInstance() throws Exception {
        try {
            if ((localWorkingDirectory == null) || !StringUtils.hasText(localWorkingDirectory)) {
                File tmp = SystemUtils.getJavaIoTmpDir();
                File sftpTmp = new File(tmp, "sftpInbound");
                this.localWorkingDirectory = "file://" + sftpTmp.getAbsolutePath();
            }

            // resource for local directory
            ResourceEditor editor = new ResourceEditor(this.resourceLoader);
            editor.setAsText(this.localWorkingDirectory);
            this.localDirectoryResource = (Resource) editor.getValue();

            fileReadingMessageSource = new FileReadingMessageSource();

            synchronizer = new SFTPInboundSynchronizer();

            CompositeFTPFileListFilter compositeFTPFileListFilter = new CompositeFTPFileListFilter();

            if (StringUtils.hasText(this.filenamePattern)) {
                PatternMatchingSFTPFileListFilter flp = new PatternMatchingSFTPFileListFilter();
                flp.setPatternExpression(this.filenamePattern);
                flp.afterPropertiesSet();
                compositeFTPFileListFilter.addFilter(flp);
            }

            if (this.filter != null) {
                compositeFTPFileListFilter.addFilter(this.filter);
            }

            synchronizer.setFilter(compositeFTPFileListFilter);

            if (null == taskScheduler) {
                Map<String, TaskScheduler> tss = null;

                if ((tss = applicationContext.getBeansOfType(TaskScheduler.class)).keySet().size() != 0) {
                    taskScheduler = tss.get(tss.keySet().iterator().next());
                }
            }

            if (null == taskScheduler) {
                ThreadPoolTaskScheduler ts = new ThreadPoolTaskScheduler();
                ts.setPoolSize(10);
                ts.setErrorHandler(new ErrorHandler() {
                        public void handleError(Throwable t) {
                            // todo make this forward a message onto the error channel (how does that work?)
                            logger.debug("error! ", t);
                        }
                    });

                ts.setWaitForTasksToCompleteOnShutdown(true);
                ts.initialize();
                this.taskScheduler = ts;
            }

            SFTPSessionFactory sessionFactory = SFTPSessionUtils.buildSftpSessionFactory(
                    this.getHost(), this.getPassword(), this.getUsername(), this.getKeyFile(), this.getKeyFilePassword(), this.getPort());

            QueuedSFTPSessionPool pool = new QueuedSFTPSessionPool(15, sessionFactory);
            pool.afterPropertiesSet();
            synchronizer.setRemotePath(this.getRemoteDirectory());
            synchronizer.setPool(pool);
            synchronizer.setAutoCreatePath(this.isAutoCreateDirectories());
            synchronizer.setShouldDeleteDownloadedRemoteFiles(this.isAutoDeleteRemoteFilesOnSync());

            SFTPMessageSource sftpMessageSource = new SFTPMessageSource(fileReadingMessageSource, synchronizer);

            sftpMessageSource.setTaskScheduler(taskScheduler);

            if (null != this.trigger) {
                sftpMessageSource.setTrigger(trigger);
            }

            sftpMessageSource.setLocalDirectory(this.localDirectoryResource);
            sftpMessageSource.afterPropertiesSet();
            sftpMessageSource.start();

            return sftpMessageSource;
        } catch (Throwable thr) {
            logger.debug("error occurred when trying to configure SFTPmessageSource ", thr);
        }

        return null;
    }
}
