package org.springframework.integration.file;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.entries.*;

import org.springframework.util.Assert;

import java.io.File;

import java.util.regex.Pattern;


/**
 * Ultimately, this factors out a lot of the common logic between the FTP and SFTP adapters
 *
 * @author Josh Long
 */
public abstract class AbstractInboundRemoteFileSystemSynchronizingMessageSource<Y, T extends AbstractInboundRemoteFileSystemSychronizer<Y>> extends AbstractEndpoint implements MessageSource<File> {
    /**
     * Extension used when downloading files. We change it right after we know it's downloaded
     */
    public static final String INCOMPLETE_EXTENSION = ".INCOMPLETE";

    /**
     * Should the endpoint attempt to create the local directory and / or the remote directory?
     */
    protected volatile boolean autoCreateDirectories = true;

    /**
     * An implementation that will handle the chores of actually syncing up the remote FS
     */
    protected volatile T synchronizer;

    /**
     * What directory should things be synced to locally ?
     */
    protected volatile Resource localDirectory;

    /**
     * The actual {@link FileReadingMessageSource} that we continue to trust to do the job monitoring the filesystem once files are moved down
     */
    protected volatile FileReadingMessageSource fileSource;

    /**
     * The predicate to use in scanning the remote Fs for downloads
     */
    protected EntryListFilter<Y> remotePredicate;

    public void setAutoCreateDirectories(boolean autoCreateDirectories) {
        this.autoCreateDirectories = autoCreateDirectories;
    }

    public void setSynchronizer(T synchronizer) {
        this.synchronizer = synchronizer;
    }

    public void setLocalDirectory(Resource localDirectory) {
        this.localDirectory = localDirectory;
    }

    public void setRemotePredicate(EntryListFilter<Y> remotePredicate) {
        this.remotePredicate = remotePredicate;
    }

    private EntryListFilter<File> buildFilter() {
        FileEntryNamer fileEntryNamer = new FileEntryNamer();
        Pattern completePattern = Pattern.compile("^.*(?<!" + INCOMPLETE_EXTENSION + ")$");
        return new CompositeEntryListFilter<File>(new AcceptOnceEntryFileListFilter<File>(), new PatternMatchingEntryListFilter<File>(fileEntryNamer, completePattern));
    }

    @Override
    protected void onInit() throws Exception {
        if (this.remotePredicate != null) {
            this.synchronizer.setFilter(this.remotePredicate);
        }

        if (this.autoCreateDirectories) {



            if((this.localDirectory != null) && !this.localDirectory.exists() && this.localDirectory.getFile().mkdirs())
                logger.debug( "the localDirectory " + this.localDirectory + " doesn't exist");
        }
        
        /**
         * Handles making sure the remote files get here in one piece
         */
        this.synchronizer.setLocalDirectory(this.localDirectory);
        this.synchronizer.setTaskScheduler(this.getTaskScheduler());
        this.synchronizer.setBeanFactory(this.getBeanFactory());
        this.synchronizer.setPhase(this.getPhase());
        this.synchronizer.setBeanName(this.getComponentName());

        /**
         * Handles forwarding files once they ultimately appear in the {@link #localDirectory}
         */
        this.fileSource = new FileReadingMessageSource();
        this.fileSource.setFilter(buildFilter());
        this.fileSource.setDirectory(this.localDirectory.getFile());
        this.fileSource.afterPropertiesSet();
        this.synchronizer.afterPropertiesSet();
    }

    public Message<File> receive() {
        return this.fileSource.receive();
    }
}
