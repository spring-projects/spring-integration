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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.file.AcceptOnceFileListFilter;
import org.springframework.integration.file.CompositeFileListFilter;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.PatternMatchingFileListFilter;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.Trigger;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import java.util.regex.Pattern;


/**
 * A source adapter for receiving files via FTP.
 *
 * @author Iwein Fuld
 */
public class FTPFileSource implements MessageSource<File>, InitializingBean, Lifecycle {
    private static final Logger logger = Logger.getLogger(FTPFileSource.class.getName());
    private FileReadingMessageSource fileSource;
    private FTPInboundSynchronizer synchronizer;

    public FTPFileSource() {
        this(new FileReadingMessageSource(), new FTPInboundSynchronizer());
    }

    public FTPFileSource(FileReadingMessageSource fileSource, FTPInboundSynchronizer synchronizer) {
        this.fileSource = fileSource;
        this.synchronizer = synchronizer;

        Pattern completePattern = Pattern.compile("^.*(?<!" + FTPInboundSynchronizer.INCOMPLETE_EXTENSION + ")$");
        fileSource.setFilter(new CompositeFileListFilter(new AcceptOnceFileListFilter(), new PatternMatchingFileListFilter(completePattern)));
    }

    public void setFileSource(FileReadingMessageSource fileSource) {
        this.fileSource = fileSource;
    }

    public void setSynchronizer(FTPInboundSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public void setLocalWorkingDirectory(Resource localWorkingDirectory) {
        this.synchronizer.setLocalDirectory(localWorkingDirectory);

        try {
            this.fileSource.setDirectory(localWorkingDirectory.getFile());
        } catch (IOException e) {
        }
    }

    public void setTrigger(Trigger trigger) {
        synchronizer.setTrigger(trigger);
    }

    public void setTaskScheduler(TaskScheduler scheduler) {
        synchronizer.setTaskScheduler(scheduler);
    }

    public void setClientPool(FTPClientPool pool) {
        synchronizer.setClientPool(pool);
    }

    public void afterPropertiesSet() throws Exception {
        synchronizer.afterPropertiesSet();
    }

    public Message<File> receive() {
        return fileSource.receive();
    }

    public void onFailure(Message<File> failedMessage, Throwable t) {
        fileSource.onFailure(failedMessage, t);
    }

    public void onSend(Message<File> sentMessage) {
        fileSource.onSend(sentMessage);
    }

    public boolean isRunning() {
        return synchronizer.isRunning();
    }

    public void start() {
        synchronizer.start();
    }

    public void stop() {
        synchronizer.stop();
    }
}
