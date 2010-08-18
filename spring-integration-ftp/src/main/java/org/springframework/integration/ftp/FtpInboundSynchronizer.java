/*
 * Copyright 2002-2008 the original author or authors.
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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ScheduledFuture;


/**
 * <code>FtpInboundSynchronizer</code> will keep a local directory in sync with a remote Ftp directory.
 * It will NOT move new files put into the local directory to the remote server.
 *
 * @author Iwein Fuld
 */
public class FtpInboundSynchronizer implements InitializingBean, Lifecycle {
    private static final Log logger = LogFactory.getLog(FtpInboundSynchronizer.class);
    static final String INCOMPLETE_EXTENSION = ".INCOMPLETE";
    private static final long DEFAULT_REFRESH_RATE = 10000;
    private volatile TaskScheduler taskScheduler;
    private volatile Trigger trigger = new PeriodicTrigger(DEFAULT_REFRESH_RATE);
    private volatile FtpClientPool clientPool;
    private volatile Resource localDirectory;
    private boolean running = false;
    private ScheduledFuture<?> scheduledFuture;
    private FtpFileListFilter filter;
    private FtpFileListFilter acceptAllFtpFileListFilter = new FtpFileListFilter() {
            public List<FTPFile> filterFiles(FTPFile[] files) {
                return Arrays.asList(files);
            }
        };

    public void setFilter(FtpFileListFilter filter) {
        this.filter = filter;
    }

    public void setTaskScheduler(TaskScheduler scheduler) {
        this.taskScheduler = scheduler;
    }

    public void setTrigger(Trigger trigger) {
        this.trigger = trigger;
    }

    public void setLocalDirectory(Resource localDirectory) {
        this.localDirectory = localDirectory;
    }

    public void setClientPool(FtpClientPool pool) {
        clientPool = pool;
    }

    public void afterPropertiesSet() throws Exception {
        Assert.notNull(localDirectory, "'localDirectory' is required.");

        if (this.filter == null) {
            this.filter = acceptAllFtpFileListFilter;
        }
    }

    private void synchronize() {
        try {
            FTPClient client = this.clientPool.getClient();
            Assert.state(client != null, FtpClientPool.class.getSimpleName() + " returned 'null' client this most likely a bug in the pool implementation.");

            Collection<FTPFile> fileList = this.filter.filterFiles(client.listFiles());

            try {
                for (FTPFile ftpFile : fileList) {
                    /*
                    * according to the FTPFile javadoc the list can contain
                    * nulls if files couldn't be parsed
                    */
                    if ((ftpFile != null) && ftpFile.isFile()) {
                        copyFileToLocalDirectory(client, ftpFile, this.localDirectory);
                    }
                }
            } finally {
                this.clientPool.releaseClient(client);
            }
        } catch (IOException e) {
            throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
        }
    }

    private boolean copyFileToLocalDirectory(FTPClient client, FTPFile ftpFile, Resource localDirectory)
        throws IOException, FileNotFoundException {
        String remoteFileName = ftpFile.getName();
        String localFileName = localDirectory.getFile().getPath() + "/" + remoteFileName;
        File localFile = new File(localFileName);

        if (!localFile.exists()) {
            String tempFileName = localFileName + INCOMPLETE_EXTENSION;
            File file = new File(tempFileName);
            FileOutputStream fos = new FileOutputStream(file);

            try {
                client.retrieveFile(remoteFileName, fos);
            } finally {
                fos.close();
            }

            file.renameTo(localFile);

            return true;
        } else {
            return false;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public void start() {
        if (running) {
            return;
        }

        Assert.state(taskScheduler != null, "'taskScheduler' is required");
        scheduledFuture = taskScheduler.schedule(new SynchronizeTask(), trigger);
        // future.get();
        this.running = true;

        if (logger.isInfoEnabled()) {
            logger.info("Started " + this);
        }
    }

    public void stop() {
        if (!running) {
            return;
        }

        this.scheduledFuture.cancel(true);
        this.running = false;

        if (logger.isInfoEnabled()) {
            logger.info("Stopped " + this);
        }
    }

    private class SynchronizeTask implements Runnable {
        public void run() {
            synchronize();
        }
    }
}
