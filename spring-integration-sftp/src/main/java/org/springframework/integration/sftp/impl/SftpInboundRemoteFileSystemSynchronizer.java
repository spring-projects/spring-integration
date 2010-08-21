package org.springframework.integration.sftp.impl;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.AbstractInboundRemoteFileSystemSychronizer;
import org.springframework.integration.file.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.sftp.SftpSession;
import org.springframework.integration.sftp.SftpSessionPool;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;


/**
 * This handles the synchronization between a remote SFTP endpoint and a local mount
 *
 * @author Josh Long
 */
public class SftpInboundRemoteFileSystemSynchronizer extends AbstractInboundRemoteFileSystemSychronizer<ChannelSftp.LsEntry> {
    /**
     * the path on the remote mount
     */
    private volatile String remotePath;

    /**
     * the pool of {@link org.springframework.integration.sftp.SftpSessionPool} SFTP sessions
     */
    private volatile SftpSessionPool clientPool;

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    @Override
    protected void onInit() throws Exception {
        Assert.notNull(this.clientPool, "'clientPool' can't be null");
        Assert.notNull(this.remotePath, "'remotePath' can't be null");
        if (this.shouldDeleteSourceFile) {
            this.entryAcknowledgmentStrategy = new DeletionEntryAcknowledgmentStrategy();
        }
    }

    @Required
    public void setClientPool(SftpSessionPool clientPool) {
        this.clientPool = clientPool;
    }

    @SuppressWarnings("ignored")
    private boolean copyFromRemoteToLocalDirectory(SftpSession sftpSession, ChannelSftp.LsEntry entry, Resource localDir)
            throws Exception {
        File fileForLocalDir = localDir.getFile();

        File localFile = new File(fileForLocalDir, entry.getFilename());

        if (!localFile.exists()) {
            InputStream in = null;
            FileOutputStream fileOutputStream = null;

            try {
                File tmpLocalTarget = new File(localFile.getAbsolutePath() +
                        AbstractInboundRemoteFileSystemSynchronizingMessageSource.INCOMPLETE_EXTENSION);

                fileOutputStream = new FileOutputStream(tmpLocalTarget);

                String remoteFqPath = this.remotePath + "/" + entry.getFilename();
                in = sftpSession.getChannel().get(remoteFqPath);
                IOUtils.copy(in, fileOutputStream);

                if (tmpLocalTarget.renameTo(localFile)) {
                    // last step
                    this.acknowledge(sftpSession, entry);
                }

                return true;
            } catch (Throwable th) {
                IOUtils.closeQuietly(in);
                IOUtils.closeQuietly(fileOutputStream);
            }
        } else {
            return true;
        }

        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void syncRemoteToLocalFileSystem() throws Exception {
        SftpSession session = null;

        try {
            session = clientPool.getSession();
            session.start();

            ChannelSftp channelSftp = session.getChannel();
            Collection<ChannelSftp.LsEntry> beforeFilter = channelSftp.ls(remotePath);
            ChannelSftp.LsEntry[] entries = (beforeFilter == null) ? new ChannelSftp.LsEntry[0] : beforeFilter.toArray(new ChannelSftp.LsEntry[beforeFilter.size()]);
            Collection<ChannelSftp.LsEntry> files = this.filter.filterEntries(entries);

            for (ChannelSftp.LsEntry lsEntry : files) {
                if ((lsEntry != null) && !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink()) {
                    copyFromRemoteToLocalDirectory(session, lsEntry, this.localDirectory);
                }
            }
        } catch (IOException e) {
            throw new MessagingException("couldn't synchronize remote to local directory", e);
        } finally {
            if ((session != null) && (clientPool != null)) {
                clientPool.release(session);
            }
        }
    }

    @Override
    protected Trigger getTrigger() {
        return new PeriodicTrigger(10 * 1000);
    }

    class DeletionEntryAcknowledgmentStrategy implements AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy<ChannelSftp.LsEntry> {
        public void acknowledge(Object useful, ChannelSftp.LsEntry msg)
                throws Exception {
            SftpSession sftpSession = (SftpSession) useful;

            String remoteFqPath = remotePath + "/" + msg.getFilename();

            sftpSession.getChannel().rm(remoteFqPath);

            if (logger.isDebugEnabled()) {
                logger.debug("deleted " + msg.getFilename());
            }
        }
    }
}
