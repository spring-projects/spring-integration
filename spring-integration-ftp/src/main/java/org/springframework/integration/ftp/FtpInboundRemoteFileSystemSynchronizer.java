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

package org.springframework.integration.ftp;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer;
import org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
import org.springframework.integration.ftp.FtpClientPool;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.util.Assert;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;

/**
 * An FTP-adapter implementation of {@link org.springframework.integration.file.synchronization.AbstractInboundRemoteFileSystemSychronizer}
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpInboundRemoteFileSystemSynchronizer extends AbstractInboundRemoteFileSystemSychronizer<FTPFile> {

	private volatile Trigger trigger = new PeriodicTrigger(10 * 1000);

	protected volatile FtpClientPool clientPool;


	@Override
	protected Trigger getTrigger() {
		return this.trigger;
	}

	/**
	 * The {@link org.springframework.integration.ftp.FtpClientPool} that holds references to {@link org.apache.commons.net.ftp.FTPClient} instances
	 *
	 * @param clientPool the {@link org.springframework.integration.ftp.FtpClientPool}
	 */
	public void setClientPool(FtpClientPool clientPool) {
		this.clientPool = clientPool;
	}

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.clientPool, "clientPool must not be null");
		if (this.shouldDeleteSourceFile) {
			this.entryAcknowledgmentStrategy = new DeletionEntryAcknowledgmentStrategy();
		}
	}

	private boolean copyFileToLocalDirectory(FTPClient client, FTPFile ftpFile, Resource localDirectory)
			throws IOException, FileNotFoundException {

		String remoteFileName = ftpFile.getName();
		String localFileName = localDirectory.getFile().getPath() + "/" + remoteFileName;
		File localFile = new File(localFileName);
		if (!localFile.exists()) {
			String tempFileName = localFileName +
					AbstractInboundRemoteFileSystemSynchronizingMessageSource.INCOMPLETE_EXTENSION;
			File file = new File(tempFileName);
			FileOutputStream fos = new FileOutputStream(file);
			try {
				client.retrieveFile(remoteFileName, fos);
				//  Perhaps we have some dispatch of the source file to do?
				acknowledge(client, ftpFile);
			}
			catch (Throwable th) {
				throw new RuntimeException(th);
			}
			finally {
				fos.close();
			}
			file.renameTo(localFile);
			return true;
		}
		return false;
	}

	@Override
	protected void syncRemoteToLocalFileSystem() {
		try {
			FTPClient client = this.clientPool.getClient();
			Assert.state(client != null,
					FtpClientPool.class.getSimpleName() +
							" returned a 'null' client. " +
							"This is most likely a bug in the pool implementation.");
			Collection<FTPFile> fileList = this.filter.filterFiles(client.listFiles());
			try {
				for (FTPFile ftpFile : fileList) {
					if ((ftpFile != null) && ftpFile.isFile()) {
						copyFileToLocalDirectory(client, ftpFile, this.localDirectory);
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


	/**
	 * An acknowledgment strategy that deletes the file.
	 */
	private class DeletionEntryAcknowledgmentStrategy implements AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy<FTPFile> {

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
