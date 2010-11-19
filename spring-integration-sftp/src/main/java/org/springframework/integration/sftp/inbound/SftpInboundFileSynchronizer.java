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
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.util.Assert;

import com.jcraft.jsch.ChannelSftp;

/**
 * Handles the synchronization between a remote SFTP directory and a local mount.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<ChannelSftp.LsEntry> {

	/**
	 * the path on the remote mount
	 */
	private volatile String remotePath;

	/**
	 * the {@link SessionFactory} for acquiring SFTP Sessions.
	 */
	private final SessionFactory sessionFactory;


	public SftpInboundFileSynchronizer(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
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

	public void synchronizeToLocalDirectory(Resource localDirectory) {
		Session session = null;
		try {
			session = this.sessionFactory.getSession();
			if (logger.isTraceEnabled()) {
				logger.trace("Pooled SftpSession " + session + " from the pool");
			}
			Collection<ChannelSftp.LsEntry> beforeFilter = session.ls(remotePath);
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
			session.close();
			if (logger.isTraceEnabled()) {
				logger.trace("Putting SftpSession " + session + " back into the pool");
			}
		}
	}

	private boolean copyFromRemoteToLocalDirectory(Session session, ChannelSftp.LsEntry entry, Resource localDir) throws Exception {
		File fileForLocalDir = localDir.getFile();
		File localFile = new File(fileForLocalDir, entry.getFilename());
		if (!localFile.exists()) {
			InputStream in = null;
			FileOutputStream fileOutputStream = null;
			try {
				File tmpLocalTarget = new File(localFile.getAbsolutePath() +
						AbstractInboundFileSynchronizingMessageSource.INCOMPLETE_EXTENSION);
				fileOutputStream = new FileOutputStream(tmpLocalTarget);
				String remoteFqPath = this.remotePath + "/" + entry.getFilename();
				in = session.get(remoteFqPath);
				try {
					IOUtils.copy(in, fileOutputStream);
				}
				finally {
					IOUtils.closeQuietly(in);
					IOUtils.closeQuietly(fileOutputStream);
				}
				if (tmpLocalTarget.renameTo(localFile)) {
					this.acknowledge(session, entry);
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


	private class DeletionEntryAcknowledgmentStrategy implements AbstractInboundFileSynchronizer.EntryAcknowledgmentStrategy<ChannelSftp.LsEntry> {

		public void acknowledge(Object useful, ChannelSftp.LsEntry msg) throws Exception {
			Session sftpSession = (Session) useful;
			String remoteFqPath = remotePath + "/" + msg.getFilename();
			sftpSession.rm(remoteFqPath);
			if (logger.isDebugEnabled()) {
				logger.debug("deleted " + msg.getFilename());
			}
		}
	}

}
