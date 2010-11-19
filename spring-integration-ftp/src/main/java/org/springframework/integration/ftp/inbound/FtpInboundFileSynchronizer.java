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

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamEvent;
import org.apache.commons.net.io.Util;

import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.integration.ftp.session.FtpClientPool;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * An FTP-adapter implementation of {@link org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer}
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<FTPFile> {

	private volatile FtpClientPool clientPool;


	/**
	 * The {@link org.springframework.integration.ftp.session.FtpClientPool} that holds references to {@link org.apache.commons.net.ftp.FTPClient} instances
	 *
	 * @param clientPool the {@link org.springframework.integration.ftp.session.FtpClientPool}
	 */
	public void setClientPool(FtpClientPool clientPool) {
		this.clientPool = clientPool;
	}

	public void afterPropertiesSet() {
		Assert.notNull(this.clientPool, "clientPool must not be null");
		if (this.shouldDeleteSourceFile) {
			this.setEntryAcknowledgmentStrategy(new DeletionEntryAcknowledgmentStrategy());
		}
	}

	public void synchronizeToLocalDirectory(Resource localDirectory) {
		try {
			FTPClient client = this.clientPool.getClient();
			Assert.state(client != null,
					FtpClientPool.class.getSimpleName() +
							" returned a 'null' client. " +
							"This is most likely a bug in the pool implementation.");
			Collection<FTPFile> fileList = this.filterFiles(client.listFiles());
			try {
				for (FTPFile ftpFile : fileList) {
					if ((ftpFile != null) && ftpFile.isFile()) {
						copyFileToLocalDirectory(client, ftpFile, localDirectory);
					}
				}
			}
			finally {
				this.clientPool.releaseClient(client);
			}
		}
		catch (IOException e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
	}

	private boolean copyFileToLocalDirectory(FTPClient client, FTPFile ftpFile, Resource localDirectory)
			throws IOException, FileNotFoundException {

		String remoteFileName = ftpFile.getName();
		String localFileName = localDirectory.getFile().getPath() + "/" + remoteFileName;
		File localFile = new File(localFileName);
		if (!localFile.exists()) {
			String tempFileName = localFileName + AbstractInboundFileSynchronizingMessageSource.INCOMPLETE_EXTENSION;
			File file = new File(tempFileName);
			FileOutputStream fos = new FileOutputStream(file);
			try {
				InputStream i = client.retrieveFileStream(remoteFileName);
				if (i != null){
					FileCopyUtils.copy(i, fos);
				}
				acknowledge(client, ftpFile);
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
				fos.close();
			}
			file.renameTo(localFile);
			return true;
		}
		return false;
	}


	/**
	 * An acknowledgment strategy that deletes the file.
	 */
	private static class DeletionEntryAcknowledgmentStrategy implements EntryAcknowledgmentStrategy<FTPFile> {

		private final Log logger = LogFactory.getLog(this.getClass());

		public void acknowledge(Object useful, FTPFile fptFile) throws Exception {
			FTPClient ftpClient = (FTPClient) useful;
			if ((fptFile != null) && ftpClient.deleteFile(fptFile.getName())) {
				if (logger.isDebugEnabled()) {
					logger.debug("deleted " + fptFile.getName());
				}
			}
		}
	}

}
