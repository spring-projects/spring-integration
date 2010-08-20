package org.springframework.integration.file;

import org.springframework.core.io.Resource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.entries.AcceptAllEntryListFilter;
import org.springframework.integration.file.entries.EntryListFilter;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

import java.util.concurrent.ScheduledFuture;


/**
 * This handles a lot of the common ground in our approach for synchronizing a remote file system locally
 *
 * @author Josh Long
 */
public abstract class AbstractInboundRemoteFileSystemSychronizer<T> extends AbstractEndpoint {
    /**
     * Should we <emphasis>delete</emphasis> the <b>source</b> file?
     * For an FTP server, for example, this would delete the original FTPFile instance
     * <p/>
     * At the moment I can simply see this triggering the setting
     */
    protected boolean shouldDeleteSourceFile;

    /**
     * the directory we're writing our synchronizations to
     */
    protected volatile Resource localDirectory;

    /**
     * a {@link org.springframework.integration.file.entries.EntryListFilter} that we're running against the <emphasis>remote</emphasis> file system view!
     */
    protected volatile EntryListFilter<T> filter = new AcceptAllEntryListFilter<T>();

    /**
     * the {@link java.util.concurrent.ScheduledFuture} instance we get when we schedule our {@link AbstractInboundRemoteFileSystemSychronizer.SynchronizeTask}
     */
    protected ScheduledFuture<?> scheduledFuture;

    /**
     * Used to store the {@link org.springframework.integration.file.AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy} implementation
     */
    protected EntryAcknowledgmentStrategy<T> entryAcknowledgmentStrategy;

    /**
     * Obviously thread safe - simply provides a NOOP impl so we don't have to keep dancing around NPE's
     */
    private EntryAcknowledgmentStrategy<T> noOpEntryAcknowledgmentStrategy = new EntryAcknowledgmentStrategy<T>() {
            public void acknowledge(Object o, T msg) {
            }
        };

    public void setEntryAcknowledgmentStrategy(EntryAcknowledgmentStrategy<T> entryAcknowledgmentStrategy) {
        this.entryAcknowledgmentStrategy = entryAcknowledgmentStrategy;
    }

    public void setShouldDeleteSourceFile(boolean shouldDeleteSourceFile) {
        this.shouldDeleteSourceFile = shouldDeleteSourceFile;
    }

    public void setLocalDirectory(Resource localDirectory) {
        this.localDirectory = localDirectory;
    }

    public void setFilter(EntryListFilter<T> filter) {
        this.filter = filter;
    }

    /**
     * @param usefulContextOrClientData
     * @param t leverages strategy implementations to enable different behavior. It's a hook to the entry ({@link T}) after it's been successfully downloaded.
     *          Conceptually, you might delete the remote one or rename it or something
     * @throws Throwable
     */
    protected void acknowledge(Object usefulContextOrClientData, T t)
        throws Throwable {
        Assert.notNull(this.entryAcknowledgmentStrategy != null, "entryAcknowledgmentStrategy can't be null!");
        this.entryAcknowledgmentStrategy.acknowledge(usefulContextOrClientData, t);
    }

    /**
     * This is the callback where we need the implementation to do some specific work
     */
    protected abstract void syncRemoteToLocalFileSystem()
            throws Exception ;

    /**
     * {@inheritDoc}
     */
    protected void doStop() {
        Assert.notNull(this.scheduledFuture, "the 'scheduledFuture' can't be null!");
        this.scheduledFuture.cancel(true);
    }

    /**
     * Returns a value in millis dictating how frequently the trigger should fire
     *
     * @return a {@link org.springframework.scheduling.Trigger} implementation (likely,
     *         {@link org.springframework.scheduling.support.PeriodicTrigger})
     */
    protected abstract Trigger getTrigger();

    /**
     * {@inheritDoc}
     */
    protected void doStart() {
        if (this.entryAcknowledgmentStrategy == null) {
            this.entryAcknowledgmentStrategy = noOpEntryAcknowledgmentStrategy;
        }

        this.scheduledFuture = this.getTaskScheduler().schedule(new SynchronizeTask(), this.getTrigger());
    }

    /**
     * Strategy interface to expose a hook for dispatching, moving, or deleting the file once it's been delivered
     *
     * @author Josh Long
     * @param <T>  the entry type (file, sftp, ftp, ...)
     */
    public static interface EntryAcknowledgmentStrategy<T> {
        /**
         * Semantics are simple. You get a pointer to the entry just processed and any kind of helper data you could ask for. Since the strategy is a
         * singleton and the clients you might ask for as context data are pooled, it's not recommended that you try to cache them.
         *
         * @param useful any context data
         * @param msg    the data / file / entry you want to process -- specific to sublcasses
         * @throws Exception thrown for any old reason
         */
        void acknowledge(Object useful, T msg) throws Exception;
    }

    /**
     * This {@link Runnable} is launched as a background thread and is used to babysit the
     * {@link org.springframework.integration.file.AbstractInboundRemoteFileSystemSychronizer#localDirectory},
     * queueing and delivering accumulated files as possible.
     */
    class SynchronizeTask implements Runnable {
        public void run() {
            try {
                syncRemoteToLocalFileSystem();
            } catch (Exception e) {
             throw new RuntimeException(e) ;
            }
        }
    }
}
