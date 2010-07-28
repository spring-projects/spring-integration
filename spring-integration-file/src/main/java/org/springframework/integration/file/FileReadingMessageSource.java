/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.file;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.core.MessageBuilder;
import org.springframework.integration.core.MessagingException;
import org.springframework.integration.message.MessageSource;
import org.springframework.util.Assert;

import java.io.File;
import java.util.*;
import java.util.concurrent.PriorityBlockingQueue;

/**
 * {@link MessageSource} that creates messages from a file system directory. To prevent messages for certain files, you
 * may supply a {@link FileListFilter}. By default, an {@link AcceptOnceFileListFilter} is used. It ensures files are
 * picked up only once from the directory.
 * <p/>
 * A common problem with reading files is that a file may be detected before it is ready. The default {@link
 * AcceptOnceFileListFilter} does not prevent this. In most cases, this can be prevented if the file-writing process
 * renames each file as soon as it is ready for reading. A pattern-matching filter that accepts only files that are
 * ready (e.g. based on a known suffix), composed with the default {@link AcceptOnceFileListFilter} would allow for
 * this. See {@link org.springframework.integration.file.CompositeFileListFilter} for a way to do this.
 * <p/>
 * A {@link Comparator} can be used to ensure internal ordering of the Files in a {@link PriorityBlockingQueue}. This
 * does not provide the same guarantees as a {@link ResequencingMessageGroupProcessor}, but in cases where writing files and failure
 * downstream are rare it might be sufficient.
 * <p/>
 * FileReadingMessageSource is fully thread-safe under concurrent <code>receive()</code> invocations and message
 * delivery callbacks.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class FileReadingMessageSource implements MessageSource<File>,
        InitializingBean {

    private static final int DEFAULT_INTERNAL_QUEUE_CAPACITY = 5;

    private static final Log logger = LogFactory
            .getLog(FileReadingMessageSource.class);

    private volatile File directory;

    private volatile DirectoryScanner scanner = new DefaultDirectoryScanner();

    private volatile boolean autoCreateDirectory = true;

    /*
     * {@link PriorityBlockingQueue#iterator()} throws {@link java.util.ConcurrentModificationException} in Java 5.
     * There is no locking around the queue, so there is also no iteration.
     */
    private final Queue<File> toBeReceived;

    private boolean scanEachPoll = false;

    /**
     * Creates a FileReadingMessageSource with a naturally ordered queue of unbounded capacity.
     */
    public FileReadingMessageSource() {
        this(null);
    }

    /**
     * Creates a FileReadingMessageSource with a bounded queue of the given capacity. This can be used to reduce the
     * memory footprint of this component when reading from a large directory.
     *
     * @param internalQueueCapacity the size of the queue used to cache files to be received internally. This queue can
     *                              be made larger to optimize the directory scanning. With scanEachPoll set to false
     *                              and the queue to a large size, it will be filled once and then completely emptied
     *                              before a new directory listing is done. This is particularly useful to reduce scans
     *                              of large numbers of files in a directory.
     */
    public FileReadingMessageSource(int internalQueueCapacity) {
        this(null);
        Assert.isTrue(internalQueueCapacity>0, "Cannot create a queue with non positive capacity");
        this.setScanner(new HeadDirectoryScanner(internalQueueCapacity));
    }

    /**
     * Creates a FileReadingMessageSource with a {@link PriorityBlockingQueue} ordered with the passed in {@link
     * Comparator}
     * <p/>
     * The size of the queue used should be large enough to hold all the files in the input directory in order to sort
     * all of them, so restricting the size of the queue is mutually exclusive with ordering. No guarantees about file
     * delivery order can be made under concurrent access.
     * <p/>
     *
     * @param receptionOrderComparator the comparator to be used to order the files in the internal queue
     */
    public FileReadingMessageSource(Comparator<File> receptionOrderComparator) {
        toBeReceived = new PriorityBlockingQueue<File>(DEFAULT_INTERNAL_QUEUE_CAPACITY,
                receptionOrderComparator);
    }

    /**
     * Specify the input directory.
     */
    public void setDirectory(File directory) {
        Assert.notNull(directory, "directory must not be null");
        this.directory = directory;
    }

    /**
     * Optionally specify a custom scanner, for example the {@link org.springframework.integration.file.RecursiveLeafOnlyDirectoryScanner}
     */
    public void setScanner(DirectoryScanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Specify whether to create the source directory automatically if it does not yet exist upon initialization. By
     * default, this value is <emphasis>true</emphasis>. If set to <emphasis>false</emphasis> and the source directory
     * does not exist, an Exception will be thrown upon initialization.
     */
    public void setAutoCreateDirectory(boolean autoCreateDirectory) {
        this.autoCreateDirectory = autoCreateDirectory;
    }

    /**
     * Sets a {@link FileListFilter}. By default a {@link AcceptOnceFileListFilter} with no bounds is used. In most
     * cases a customized {@link FileListFilter} will be needed to deal with modification and duplication concerns. If
     * multiple filters are required a {@link CompositeFileListFilter} can be used to group them together.
     * <p/>
     * <b>The supplied filter must be thread safe.</b>.
     */
    public void setFilter(FileListFilter filter) {
        Assert.notNull(filter, "'filter' must not be null");
        this.scanner.setFilter(filter);
    }

    /**
     * Optional. Sets a {@link org.springframework.integration.file.locking.FileLocker} to be used to guard files
     * against duplicate processing.
     * <p/>
     * <b>The supplied FileLocker must be thread safe</b>
     */
    public void setLocker(FileLocker locker) {
        Assert.notNull(locker, "'fileLocker' must not be null.");
        this.scanner.setLocker(locker);
    }

    /**
     * Optional. Set this flag if you want to make sure the internal queue is refreshed with the latest content of the
     * input directory on each poll.
     * <p/>
     * By default this implementation will empty its queue before looking at the directory again. In cases where order
     * is relevant it is important to consider the effects of setting this flag. The internal {@link
     * java.util.concurrent.BlockingQueue} that this class is keeping will more likely be out of sync with the file
     * system if this flag is set to <code>false</code>, but it will change more often (causing expensive reordering) if
     * it is set to <code>true</code>.
     */
    public void setScanEachPoll(boolean scanEachPoll) {
        this.scanEachPoll = scanEachPoll;
    }

    public final void afterPropertiesSet() {
        Assert.notNull(directory, "'directory' must not be set before initialization");
        if (!this.directory.exists() && this.autoCreateDirectory) {
            this.directory.mkdirs();
        }
        Assert.isTrue(this.directory.exists(), "Source directory ["
                + directory + "] does not exist.");
        Assert.isTrue(this.directory.isDirectory(), "Source path ["
                + this.directory + "] does not point to a directory.");
        Assert.isTrue(this.directory.canRead(), "Source directory ["
                + this.directory + "] is not readable.");
    }

    public Message<File> receive() throws MessagingException {
        Message<File> message = null;
        // rescan only if needed or explicitly configured
        if (scanEachPoll || toBeReceived.isEmpty()) {
            scanInputDirectory();
        }
        File file = toBeReceived.poll();
        // file == null means the queue was empty
        // we can't rely on isEmpty for concurrency reasons
        while (file != null && !scanner.tryClaim(file)) {
            file = toBeReceived.poll();
        }
        if (file != null) {
            message = MessageBuilder.withPayload(file).build();
            if (logger.isInfoEnabled()) {
                logger.info("Created message: [" + message + "]");
            }
        }
        return message;
    }

    private void scanInputDirectory() {
        List<File> filteredFiles = scanner.listFiles(directory);
        Set<File> freshFiles = new HashSet<File>(filteredFiles);
        if (!freshFiles.isEmpty()) {
            toBeReceived.addAll(freshFiles);
            if (logger.isDebugEnabled()) {
                logger.debug("Added to queue: " + freshFiles);
            }
        }
    }

    /**
     * Adds the failed message back to the 'toBeReceived' queue if there is room.
     */
    public void onFailure(Message<File> failedMessage, Throwable t) {
        if (logger.isWarnEnabled()) {
            logger.warn("Failed to send: " + failedMessage);
        }
        toBeReceived.offer(failedMessage.getPayload());
    }

    /**
     * The message is just logged. It was already removed from the queue during the call to <code>receive()</code>
     */
    public void onSend(Message<File> sentMessage) {
        if (logger.isDebugEnabled()) {
            logger.debug("Sent: " + sentMessage);
        }
    }
}
