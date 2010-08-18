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
import java.util.regex.Pattern;


/**
 * this creates the message source that ultimately 'see's files on a local directory and forwards them on to the bus.
 * These files are asynchronously deposited into a folder via the SFTP synchronizer. This code is <i>very</i> influenced
 * by the FtpFileSource class from the Spring Integration FTP adapter.
 *
 * @author Josh Long
 */
public class SftpMessageSource implements MessageSource<File>, InitializingBean, Lifecycle {
    private FileReadingMessageSource fileReadingMessageSource;
    private Resource localDirectory;
    private SftpInboundSynchronizer synchronizer;
    private TaskScheduler taskScheduler;
    private Trigger trigger;

    public SftpMessageSource(FileReadingMessageSource fileSource, SftpInboundSynchronizer synchronizer) {
        this.fileReadingMessageSource = fileSource;
        this.synchronizer = synchronizer;

        Pattern completePattern = Pattern.compile("^.*(?<!" + SftpInboundSynchronizer.INCOMPLETE_EXTENSION + ")$");
        fileReadingMessageSource.setFilter(new CompositeFileListFilter(new AcceptOnceFileListFilter(), new PatternMatchingFileListFilter(completePattern)));
    }

    public void afterPropertiesSet() throws Exception {
        synchronizer.afterPropertiesSet();
        this.fileReadingMessageSource.afterPropertiesSet();
    }

    public FileReadingMessageSource getFileReadingMessageSource() {
        return fileReadingMessageSource;
    }

    public Resource getLocalDirectory() {
        return localDirectory;
    }

    public SftpInboundSynchronizer getSynchronizer() {
        return synchronizer;
    }

    public TaskScheduler getTaskScheduler() {
        return taskScheduler;
    }

    public Trigger getTrigger() {
        return trigger;
    }

    public boolean isRunning() {
        return this.synchronizer.isRunning();
    }

    public Message<File> receive() {
        return this.fileReadingMessageSource.receive();
    }

    public void setFileReadingMessageSource(final FileReadingMessageSource fileReadingMessageSource) {
        this.fileReadingMessageSource = fileReadingMessageSource;
    }

    public void setLocalDirectory(final Resource localDirectory) {
        this.localDirectory = localDirectory;
        try {
            this.fileReadingMessageSource.setDirectory(localDirectory.getFile());
        } catch (IOException e) {
            
        }
        this.synchronizer.setLocalDirectory(localDirectory);
    }

    public void setSynchronizer(final SftpInboundSynchronizer synchronizer) {
        this.synchronizer = synchronizer;
    }

    public void setTaskScheduler(final TaskScheduler taskScheduler) {
        this.taskScheduler = taskScheduler;
        synchronizer.setTaskScheduler(taskScheduler);
    }

    public void setTrigger(final Trigger trigger) {
        this.trigger = trigger;
        this.synchronizer.setTrigger(trigger);
    }

    public void start() {
        synchronizer.start();
    }

    public void stop() {
        synchronizer.stop();
    }
}
