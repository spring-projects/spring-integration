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

package org.springframework.integration.ftp.inbound;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.MessagingException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;

/**
 * An FTP-adapter implementation of {@link org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer}
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<FTPFile> {

	private volatile String remotePath;

	private volatile SessionFactory sessionFactory;


	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 */
	public FtpInboundFileSynchronizer(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.sessionFactory, "sessionFactory must not be null");
	}

	public void synchronizeToLocalDirectory(File localDirectory) {
		Session session = null;
		try {
			session = this.sessionFactory.getSession();
			Assert.state(session != null, "failed to acquire an FTP Session");
			Collection<FTPFile> files = session.ls(this.remotePath);
			if (!CollectionUtils.isEmpty(files)) {
				Collection<FTPFile> filteredFiles = this.filterFiles(files.toArray(new FTPFile[]{}));
				for (FTPFile ftpFile : filteredFiles) {
					if ((ftpFile != null) && ftpFile.isFile()) {
						copyFileToLocalDirectory(session, ftpFile, localDirectory);
					}
				}
			}
		}
		catch (IOException e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
		finally {
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception ignored) {
				}
			}
		}
	}

	private boolean copyFileToLocalDirectory(Session session, FTPFile ftpFile, File localDirectory)
			throws IOException, FileNotFoundException {

		String remoteFileName = ftpFile.getName();
		String localFileName = localDirectory.getPath() + "/" + remoteFileName;
		File localFile = new File(localFileName);
		if (!localFile.exists()) {
			String tempFileName = localFileName + AbstractInboundFileSynchronizingMessageSource.INCOMPLETE_EXTENSION;
			File file = new File(tempFileName);
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			try {
				InputStream inputStream = session.get(remoteFileName);
				if (inputStream == null) {
					return false;
				}
				FileCopyUtils.copy(inputStream, fileOutputStream);
				if (this.shouldDeleteSourceFile) {
					this.deleteRemoteFile(session, ftpFile);
				}
			}
			catch (Exception e) {
				if (e instanceof RuntimeException){
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failed to copy file", e);
				}
			}
			finally {
				fileOutputStream.close();
			}
			file.renameTo(localFile);
			return true;
		}
		return false;
	}

	// TODO: make this an abstract method in the base class once the code that calls this is refactored upward
	private void deleteRemoteFile(Session session, FTPFile ftpFile) {
		if ((ftpFile != null) && session.rm(ftpFile.getName())) {
			if (logger.isDebugEnabled()) {
				logger.debug("deleted " + ftpFile.getName());
			}
		}
	}

}
