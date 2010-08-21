package org.springframework.integration.sftp.impl;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

import org.springframework.integration.file.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.sftp.SftpSession;
import org.springframework.integration.sftp.SftpSessionPool;

import org.springframework.util.Assert;


/**
 * a {@link org.springframework.integration.core.MessageSource} implementation for SFTP
 *
 * @author Josh Long
 */
public class SftpInboundRemoteFileSystemSynchronizingMessageSource extends AbstractInboundRemoteFileSystemSynchronizingMessageSource<ChannelSftp.LsEntry, SftpInboundRemoteFileSystemSynchronizer> {
    /**
     * the pool of sessions
     */
    private volatile SftpSessionPool clientPool;

 
    /**
     * the remote path on teh server
     */
    private volatile String remotePath;

    public void setClientPool(SftpSessionPool clientPool) {
        this.clientPool = clientPool;
    }

    public void setRemotePath(String remotePath) {
        this.remotePath = remotePath;
    }

    @Override
    protected void doStart() {
        this.synchronizer.start();
    }

    @Override
    protected void doStop() {
        this.synchronizer.stop();
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
        SftpSession session = null;
        ChannelSftp channelSftp = null;

        try {
            session = this.clientPool.getSession();
            Assert.state(session != null, "session as returned from the pool should not be null. " + "If it is, it is most likely an error in the pool implementation.  ");
            session.start();
            channelSftp = session.getChannel();

            SftpATTRS attrs = channelSftp.stat(remotePath);
            assert (attrs != null) && attrs.isDir() : "attrs can't be null, and should indicate that it's a directory!";

            return true;
        } catch (Throwable th) {
            if (this.autoCreateDirectories && (this.clientPool != null) && (session != null)) {
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
            if ((clientPool != null) && (session != null)) {
                clientPool.release(session);
            }
        }

        return false;
    }

    @Override
    protected void onInit() throws Exception {
        super.onInit();

        this.checkThatRemotePathExists(this.remotePath);
        this.synchronizer.setClientPool(this.clientPool);
    }
}
