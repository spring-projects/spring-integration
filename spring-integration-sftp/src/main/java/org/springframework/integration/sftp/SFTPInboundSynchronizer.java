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
package org.springframework.integration.sftp;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;
import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


/**
 * This handles keeping the {@link #localDirectory} in sync with the contents of the remote mount. From there, files are deposited
 * into a folder where the {@link org.springframework.integration.file.FileReadingMessageSource} will eventually deliver them as events
 *
 * @author Josh Long
 * @author Mario Gray
 */
public class SFTPInboundSynchronizer implements InitializingBean {
    private static final long DEFAULT_REFRESH_RATE = 10 * 1000; // 10 seconds

    // a lot of the approach for this (including the use of a FileReadingMessageSource and the regex / mask approach were lifted from FtpInboundSynchronizer
    static final String INCOMPLETE_EXTENSION = ".INCOMPLETE";
    private Log logger = LogFactory.getLog(getClass());
    private volatile Resource localDirectory;
    private volatile SFTPSessionPool pool;
    private volatile ScheduledFuture<?> scheduledFuture;
    private volatile String remotePath;
    private volatile TaskScheduler taskScheduler;
    private volatile Trigger trigger = new PeriodicTrigger(DEFAULT_REFRESH_RATE);
    private volatile boolean autoCreatePath;
    private volatile boolean running;
    private SFTPFileListFilter filter;

    public void setFilter(SFTPFileListFilter filter) {
        this.filter = filter;
    }

    private volatile boolean shouldDeleteDownloadedRemoteFiles; //.. this is false

    private SFTPFileListFilter acceptAllFilteListFilter = new SFTPFileListFilter(){
        public List<ChannelSftp.LsEntry> filterFiles(ChannelSftp.LsEntry[] files) {
           return Arrays.asList( files);
        }
    } ;

    public void afterPropertiesSet() throws Exception {
        Assert.state(taskScheduler != null, "taskScheduler can't be null!");
        Assert.state(localDirectory != null, "the localDirectory property must not be null!");

        File localDir = localDirectory.getFile();

        if (!localDir.exists()) {
            if (autoCreatePath) {
                if (!localDir.mkdirs()) {
                    throw new RuntimeException(String.format("couldn't create localDirectory %s", this.localDirectory.getFile().getAbsolutePath()));
                }
            }
        }

        if(this.filter == null)
            this.filter = acceptAllFilteListFilter;

    }

    public boolean isRunning() {
        return running;
    }

    public boolean isShouldDeleteDownloadedRemoteFiles() {
        return shouldDeleteDownloadedRemoteFiles;
    }

    public void setAutoCreatePath(boolean autoCreatePath) {
        this.autoCreatePath = autoCreatePath;
    }

    public void setLocalDirectory(Resource localDirectory) {
        this.localDirectory = localDirectory;
    }

    public void setPool(SFTPSessionPool pool) {
        this.pool = pool;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    public void setScheduledFuture(ScheduledFuture<?> scheduledFuture) {
        this.scheduledFuture = scheduledFuture;
    }

    public void setShouldDeleteDownloadedRemoteFiles(boolean shouldDeleteDownloadedRemoteFiles) {
        this.shouldDeleteDownloadedRemoteFiles = shouldDeleteDownloadedRemoteFiles;
    }

    public void setTaskScheduler(TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
    }

    public void setTrigger(Trigger t) {
        this.trigger = t;
    }

    public void start() {
        if (running) {
            return;
        }

        Assert.state(checkThatRemotePathExists(remotePath), "the remotePath should exist before we can sync with it!");
        Assert.state(taskScheduler != null, "'taskScheduler' is required");

        scheduledFuture = taskScheduler.schedule(new SynchronizeTask(), trigger);

        this.running = true;
    }

    public void stop() {
        if (!running) {
            return;
        }

        Assert.state(scheduledFuture != null, "scheduledFuture is null!");
        this.scheduledFuture.cancel(true);
        this.running = false;
    }

    @SuppressWarnings("unchecked")
    public void synchronize() throws Exception {
        SFTPSession session = null;

        try {
            session = pool.getSession();
            session.start();

            ChannelSftp channelSftp = session.getChannel();
            Collection<ChannelSftp.LsEntry> beforeFilter = channelSftp.ls(remotePath);
            ChannelSftp.LsEntry [] entries = beforeFilter == null? new ChannelSftp.LsEntry[0] :
                    beforeFilter.toArray(new ChannelSftp.LsEntry[ beforeFilter.size()]) ;
            Collection<ChannelSftp.LsEntry> files =  this.filter.filterFiles( entries );

            for (ChannelSftp.LsEntry lsEntry : files) {
                if ((lsEntry != null) && !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink()) {
                    copyFromRemoteToLocalDirectory(session, lsEntry, this.localDirectory);
                }
            }
        } catch (IOException e) {
            throw new MessagingException("couldn't synchronize remote to local directory", e);
        } finally {
            if ((session != null) && (pool != null)) {
                pool.release(session);
            }
        }
    }

    /**
     * there be dragons this way ... This method will check to ensure that the remote directory exists. If the directory
     * doesnt exist, and autoCreatePath is 'true,' then this method makes a few reasonably sane attempts
     * to create it. Otherwise, it fails fast.
     *
     * @param remotePath the path on the remote SSH / SFTP server to create.
     * @return whether or not the directory is there (regardless of whether we created it in this method or it already
     *         existed.)
     */
    private boolean checkThatRemotePathExists(String remotePath) {
        SFTPSession session = null;
        ChannelSftp channelSftp = null;

        try {
            session = pool.getSession();
            Assert.state(session != null, "session as returned from the pool should not be null. " + "If it is, it is most likely an error in the pool implementation.  ");
            session.start();
            channelSftp = session.getChannel();

            SftpATTRS attrs = channelSftp.stat(remotePath);
            assert (attrs != null) && attrs.isDir() : "attrs can't be null, and should indicate that it's a directory!";

            return true;
        } catch (Throwable th) {
            if (this.autoCreatePath && (pool != null) && (session != null)) {
                try {
                    if (channelSftp != null) {
                        channelSftp.mkdir(remotePath);

                        if (channelSftp.stat(remotePath).isDir()) {
                            return true;
                        }
                    }
                } catch (Throwable t) {
                    return false;
                }
            }
        } finally {
            if ((pool != null) && (session != null)) {
                pool.release(session);
            }
        }

        return false;
    }

    @SuppressWarnings("ignored")
    private boolean copyFromRemoteToLocalDirectory(SFTPSession sftpSession, ChannelSftp.LsEntry entry, Resource localDir)
        throws Exception {
        File fileForLocalDir = localDir.getFile();

        File localFile = new File(fileForLocalDir, entry.getFilename());

        if (!localFile.exists()) {
            InputStream in = null;
            FileOutputStream fos = null;

            try {
                File tmpLocalTarget = new File(localFile.getAbsolutePath() + INCOMPLETE_EXTENSION);

                fos = new FileOutputStream(tmpLocalTarget);

                String remoteFqPath = this.remotePath + "/" + entry.getFilename();
                in = sftpSession.getChannel().get(remoteFqPath);
                IOUtils.copy(in, fos);

                if (tmpLocalTarget.renameTo(localFile)) {
                    // last step
                    if (isShouldDeleteDownloadedRemoteFiles()) {
                        sftpSession.getChannel().rm(remoteFqPath);
                    }
                }

                return true;
            } catch (Throwable th) {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(fos);
            }
        } else {
            return true;
        }

        return false;
    }

    class SynchronizeTask implements Runnable {
        public void run() {
            try {
                synchronize();
            } catch (Throwable e) {
                // todo   logger.debug("couldn't invoke synchronize()", e);
            }
        }
    }
}
