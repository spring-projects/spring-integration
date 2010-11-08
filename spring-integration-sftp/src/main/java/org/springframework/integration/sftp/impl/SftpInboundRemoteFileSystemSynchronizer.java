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

package org.springframework.integration.sftp.impl;

import com.jcraft.jsch.ChannelSftp;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
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
 * Gandles the synchronization between a remote SFTP endpoint and a local mount.
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

	@Required
	public void setClientPool(SftpSessionPool clientPool) {
		this.clientPool = clientPool;
	}

	@Override
	protected Trigger getTrigger() {
		return new PeriodicTrigger(10 * 1000);
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.clientPool, "'clientPool' must not be null");
		Assert.notNull(this.remotePath, "'remotePath' must not be null");
		if (this.shouldDeleteSourceFile) {
			this.entryAcknowledgmentStrategy = new DeletionEntryAcknowledgmentStrategy();
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
			catch (Throwable th) {
				logger.error("failure occurred while copying from remote to local directory", th);
			}
		}
		else {
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
			Collection<ChannelSftp.LsEntry> files = this.filter.filterFiles(entries);
			for (ChannelSftp.LsEntry lsEntry : files) {
				if ((lsEntry != null) && !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink()) {
					copyFromRemoteToLocalDirectory(session, lsEntry, this.localDirectory);
				}
			}
		}
		catch (IOException e) {
			throw new MessagingException("couldn't synchronize remote to local directory", e);
		}
		finally {
			if ((session != null) && (clientPool != null)) {
				clientPool.release(session);
			}
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
