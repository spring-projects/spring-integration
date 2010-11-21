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
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.springframework.integration.MessagingException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;

/**
 * Handles the synchronization between a remote SFTP directory and a local mount.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class SftpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<ChannelSftp.LsEntry> {

	public SftpInboundFileSynchronizer(SessionFactory sessionFactory) {
		super(sessionFactory);
	}


	@Override
	protected void synchronizeToLocalDirectory(String remoteDirectoryPath, File localDirectory, Session session) throws IOException {
		Collection<ChannelSftp.LsEntry> beforeFilter = session.ls(remoteDirectoryPath);
		ChannelSftp.LsEntry[] entries = (beforeFilter == null) ? new ChannelSftp.LsEntry[0] : 
				beforeFilter.toArray(new ChannelSftp.LsEntry[beforeFilter.size()]);
		Collection<ChannelSftp.LsEntry> files = this.filterFiles(entries);
		for (ChannelSftp.LsEntry lsEntry : files) {
			if ((lsEntry != null) && !lsEntry.getAttrs().isDir() && !lsEntry.getAttrs().isLink()) {
				copyFromRemoteToLocalDirectory(remoteDirectoryPath, lsEntry, localDirectory, session);
			}
		}
	}

	private boolean copyFromRemoteToLocalDirectory(String remoteDirectoryPath, ChannelSftp.LsEntry entry, File localDirectory, Session session) throws IOException {
		File localFile = new File(localDirectory, entry.getFilename());
		if (!localFile.exists()) {
			InputStream in = null;
			FileOutputStream fileOutputStream = null;
			try {
				File tmpLocalTarget = new File(localFile.getAbsolutePath() +
						AbstractInboundFileSynchronizingMessageSource.INCOMPLETE_EXTENSION);
				fileOutputStream = new FileOutputStream(tmpLocalTarget);
				String remoteFqPath = remoteDirectoryPath + File.separator + entry.getFilename();
				in = session.get(remoteFqPath);
				try {
					FileCopyUtils.copy(in, fileOutputStream);
				}
				finally {
					in.close();
					fileOutputStream.close();
				}
				if (tmpLocalTarget.renameTo(localFile)) {
					if (this.shouldDeleteSourceFile) {
						this.deleteRemoteFile(remoteDirectoryPath, session, entry);
					}
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

	private void deleteRemoteFile(String remotePath, Session session, ChannelSftp.LsEntry msg) {
		String remoteFqPath = remotePath + "/" + msg.getFilename();
		session.rm(remoteFqPath);
		if (logger.isDebugEnabled()) {
			logger.debug("deleted " + msg.getFilename());
		}
	}

}
