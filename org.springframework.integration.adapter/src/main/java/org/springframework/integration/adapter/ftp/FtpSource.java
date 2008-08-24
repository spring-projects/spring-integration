/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.ftp;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.integration.adapter.file.AbstractDirectorySource;
import org.springframework.integration.adapter.file.Backlog;
import org.springframework.integration.adapter.file.FileSnapshot;
import org.springframework.integration.message.MessageCreator;
import org.springframework.util.Assert;

/**
 * A source adapter for receiving files via FTP.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class FtpSource extends AbstractDirectorySource<List<File>> {

	private volatile File localWorkingDirectory;

	private volatile int maxFilesPerMessage = -1;

	private final FTPClientPool clientPool;

	public FtpSource(MessageCreator<List<File>, List<File>> messageCreator, FTPClientPool clientPool) {
		super(messageCreator);
		this.clientPool = clientPool;
	}

	public void setMaxFilesPerMessage(int maxFilesPerMessage) {
		Assert.isTrue(maxFilesPerMessage > 0, "'maxFilesPerMessage' must be greater than 0");
		this.maxFilesPerMessage = maxFilesPerMessage;
	}

	public void setLocalWorkingDirectory(File localWorkingDirectory) {
		Assert.notNull(localWorkingDirectory, "'localWorkingDirectory' must not be null");
		this.localWorkingDirectory = localWorkingDirectory;
	}

	@Override
	protected void refreshSnapshotAndMarkProcessing(Backlog<FileSnapshot> directoryContentManager) throws IOException {
		List<FileSnapshot> snapshot = new ArrayList<FileSnapshot>();
		synchronized (directoryContentManager) {
			populateSnapshot(snapshot);
			directoryContentManager.processSnapshot(snapshot);
			directoryContentManager.prepareForProcessing(maxFilesPerMessage);
		}
	}

	@Override
	protected void populateSnapshot(List<FileSnapshot> snapshot) throws IOException {
		FTPClient client = this.clientPool.getClient();
		FTPFile[] fileList = client.listFiles();
		try {
			for (FTPFile ftpFile : fileList) {
				/*
				 * according to the FTPFile javadoc the list can contain nulls
				 * if files couldn't be parsed
				 */
				if (ftpFile != null) {
					FileSnapshot fileSnapshot = new FileSnapshot(ftpFile.getName(), ftpFile.getTimestamp()
							.getTimeInMillis(), ftpFile.getSize());
					snapshot.add(fileSnapshot);
				}
			}
		}
		finally {
			this.clientPool.releaseClient(client);
		}
	}

	protected List<File> retrieveNextPayload() throws IOException {
		FTPClient client = this.clientPool.getClient();
		try {
			List<File> files = new ArrayList<File>();
			List<FileSnapshot> toDo = this.getBacklog().getProcessingBuffer();
			for (FileSnapshot fileSnapshot : toDo) {
				//some awkwardness here because the local path may be different from the remote path
				File file = new File(this.localWorkingDirectory, fileSnapshot.getFileName());
				if (file.exists()) {
					file.delete();
				}
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				client.retrieveFile(fileSnapshot.getFileName(), fileOutputStream);
				fileOutputStream.close();
				files.add(file);
			}
			return files;
		}
		finally {
			this.clientPool.releaseClient(client);
		}
	}

}
