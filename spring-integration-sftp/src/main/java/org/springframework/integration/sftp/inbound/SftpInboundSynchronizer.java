/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.sftp.inbound;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.io.IOUtils;

import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.sftp.session.SftpSession;
import org.springframework.integration.sftp.session.SftpSessionPool;
import org.springframework.util.Assert;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.SftpATTRS;

/**
 * Handles the synchronization between a remote SFTP directory and a local mount.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpInboundSynchronizer extends AbstractInboundRemoteFileSystemSychronizer<ChannelSftp.LsEntry> {

	/**
	 * the path on the remote mount
	 */
	private volatile String remotePath;

	private volatile boolean autoCreateDirectories;

	/**
	 * the pool of {@link org.springframework.integration.sftp.session.SftpSessionPool} SFTP sessions
	 */
	private final SftpSessionPool sessionPool;


	public SftpInboundSynchronizer(SftpSessionPool sessionPool) {
		Assert.notNull(sessionPool, "'sessionPool' must not be null");
		this.sessionPool = sessionPool;
	}


	public void setAutoCreateDirectories(boolean autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.remotePath, "'remotePath' must not be null");
		if (this.shouldDeleteSourceFile) {
			this.setEntryAcknowledgmentStrategy(new DeletionEntryAcknowledgmentStrategy());
		}
	}
	
	/**
	 * This method will check to ensure that the remote directory exists. If the directory
	 * doesnt exist, and autoCreatePath is 'true,' then this method makes a few reasonably sane attempts
	 * to create it. Otherwise, it fails fast.
	 *
	 * @param remotePath the path on the remote SSH / SFTP server to create.
	 * @return whether or not the directory is there (regardless of whether we created it in this method or it already
	 *         existed.)
	 */
	private boolean checkThatRemotePathExists(String remotePath, SftpSession session) {
		ChannelSftp channelSftp = session.getChannel();
		try {
			SftpATTRS attrs = channelSftp.stat(remotePath);
			assert (attrs != null) && attrs.isDir() : "attrs can't be null, and should indicate that it's a directory!";
			return true;
		} 
		catch (Throwable th) {
			if (this.autoCreateDirectories && (this.sessionPool != null) && (session != null)) {
				try {
					if (channelSftp != null) {
						channelSftp.mkdir(remotePath);

						if (channelSftp.stat(remotePath).isDir()) {
							return true;
						}
					}
				} 
				catch (RuntimeException re) {
					throw re;
				}
				catch (Exception e){
					throw new MessagingException("Failed to auto-create remote directory", e);
				}

			}
		} 
		return false;
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void syncRemoteToLocalFileSystem(Resource localDirectory) {
		SftpSession session = null;
		try {
			session = sessionPool.getSession();
			logger.trace("Pooled SftpSession " + this.sessionPool + " from the pool");
			session.connect();
			this.checkThatRemotePathExists(remotePath, session);
			ChannelSftp channelSftp = session.getChannel();
			Collection<ChannelSftp.LsEntry> beforeFilter = channelSftp.ls(remotePath);
			ChannelSftp.LsEntry[] entries = (beforeFilter == null) ? new ChannelSftp.LsEntry[0] : 
				beforeFilter.toArray(new ChannelSftp.LsEntry[beforeFilter.size()]);
			Collection<ChannelSftp.LsEntry> files = this.filterFiles(entries);
			for (ChannelSftp.LsEntry lsEntry : files) {
				if ((lsEntry != null) && !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink()) {
					copyFromRemoteToLocalDirectory(session, lsEntry, localDirectory);
				}
			}
		}
		catch (Exception e) {
			throw new MessagingException("couldn't synchronize remote to local directory", e);
		}
		finally {
			this.sessionPool.release(session);
			logger.trace("Putting SftpSession " + this.sessionPool + " back into the pool");
		}
	}

	private boolean copyFromRemoteToLocalDirectory(SftpSession sftpSession, ChannelSftp.LsEntry entry, Resource localDir) throws Exception {
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
				try {
					IOUtils.copy(in, fileOutputStream);
				}
				finally {
					IOUtils.closeQuietly(in);
					IOUtils.closeQuietly(fileOutputStream);
				}
				if (tmpLocalTarget.renameTo(localFile)) {
					this.acknowledge(sftpSession, entry);
				}
				return true;
			}
			catch (Exception e) {
				if (e instanceof RuntimeException){
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
		}
		else {
			return true;
		}
	}


	private class DeletionEntryAcknowledgmentStrategy implements AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy<ChannelSftp.LsEntry> {

		public void acknowledge(Object useful, ChannelSftp.LsEntry msg) throws Exception {
			SftpSession sftpSession = (SftpSession) useful;
			String remoteFqPath = remotePath + "/" + msg.getFilename();
			sftpSession.getChannel().rm(remoteFqPath);
			if (logger.isDebugEnabled()) {
				logger.debug("deleted " + msg.getFilename());
			}
		}
	}

}
