package org.springframework.integration.ftp.impl;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.AbstractInboundRemoteFileSystemSychronizer;
import org.springframework.integration.file.AbstractInboundRemoteFileSystemSynchronizingMessageSource;
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
 * An FTP-adapter implementation of {@link org.springframework.integration.file.AbstractInboundRemoteFileSystemSychronizer}
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class FtpInboundRemoteFileSystemSynchronizer extends AbstractInboundRemoteFileSystemSychronizer<FTPFile> {
	protected FtpClientPool clientPool;

	@Override
	protected void onInit() throws Exception {
		Assert.notNull(this.clientPool, "clientPool can't be null");

		if (this.shouldDeleteSourceFile) {
			this.entryAcknowledgmentStrategy = new DeletionEntryAcknowledgmentStrategy();
		}
	}

	/**
	 * The {@link org.springframework.integration.ftp.FtpClientPool} that holds references to {@link org.apache.commons.net.ftp.FTPClient} instances
	 *
	 * @param clientPool the {@link org.springframework.integration.ftp.FtpClientPool}
	 */
	public void setClientPool(FtpClientPool clientPool) {
		this.clientPool = clientPool;
	}

	protected boolean copyFileToLocalDirectory(FTPClient client, FTPFile ftpFile, Resource localDirectory)
			throws IOException, FileNotFoundException {
		String remoteFileName = ftpFile.getName();
		String localFileName = localDirectory.getFile().getPath() + "/" + remoteFileName;
		File localFile = new File(localFileName);

		if (!localFile.exists()) {
			String tempFileName = localFileName + AbstractInboundRemoteFileSystemSynchronizingMessageSource.INCOMPLETE_EXTENSION;
			File file = new File(tempFileName);
			FileOutputStream fos = new FileOutputStream(file);

			try {
				client.retrieveFile(remoteFileName, fos);

				//  Perhaps we have some dispatch of hte source file to do?
				acknowledge(client, ftpFile);
			} catch (Throwable th) {
				throw new RuntimeException(th);
			} finally {
				fos.close();
			}

			file.renameTo(localFile);

			return true;
		} else {
			return false;
		}
	}

	@Override
	protected void syncRemoteToLocalFileSystem() {
		try {
			FTPClient client = this.clientPool.getClient();
			Assert.state(client != null, FtpClientPool.class.getSimpleName() + " returned a 'null' client. " + "This most likely a bug in the pool implementation.");

			Collection<FTPFile> fileList = this.filter.filterEntries(client.listFiles());

			try {
				for (FTPFile ftpFile : fileList) {
					if ((ftpFile != null) && ftpFile.isFile()) {
						copyFileToLocalDirectory(client, ftpFile, this.localDirectory);
					}
				}
			} finally {
				this.clientPool.releaseClient(client);
			}
		} catch (IOException e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
	}

	@Override
	protected Trigger getTrigger() {
		return new PeriodicTrigger(10 * 1000);
	}

	/**
	 * An ackowledgment strategy that deletes
	 */
	class DeletionEntryAcknowledgmentStrategy implements AbstractInboundRemoteFileSystemSychronizer.EntryAcknowledgmentStrategy<FTPFile> {
		public void acknowledge(Object useful, FTPFile msg)
				throws Exception {
			FTPClient ftpClient = (FTPClient) useful;
			if ((msg != null) && ftpClient.deleteFile(msg.getName())) {
				if (logger.isDebugEnabled()) {
					logger.debug("deleted " + msg.getName());
				}
			}
		}
	}
}
