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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.adapter.file.AbstractDirectorySource;
import org.springframework.integration.adapter.file.Backlog;
import org.springframework.integration.adapter.file.FileInfo;
import org.springframework.integration.message.Message;
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
	protected void refreshSnapshotAndMarkProcessing(Backlog<FileInfo> directoryContentManager) throws IOException {
		Map<String, FileInfo> snapshot = new HashMap<String, FileInfo>();
		synchronized (directoryContentManager) {
			populateSnapshot(snapshot);
			directoryContentManager.processSnapshot(snapshot);
			ArrayList<String> backlog = new ArrayList<String>(directoryContentManager.getBacklog().keySet());
			int toIndex = this.maxFilesPerMessage == -1 ? backlog.size() : Math.min(this.maxFilesPerMessage, backlog.size());
			directoryContentManager.itemProcessing(backlog.subList(0, toIndex).toArray(new String[] {}));
		}
	}

	@Override
	protected void populateSnapshot(Map<String, FileInfo> snapshot) throws IOException {
		FTPClient client = this.clientPool.getClient();
		FTPFile[] fileList = client.listFiles();
		try {
			for (FTPFile ftpFile : fileList) {
				/*
				 * according to the FTPFile javadoc the list can contain nulls
				 * if files couldn't be parsed
				 */
				if (ftpFile != null) {
					FileInfo fileInfo = new FileInfo(ftpFile.getName(), ftpFile.getTimestamp().getTimeInMillis(),
							ftpFile.getSize());
					snapshot.put(ftpFile.getName(), fileInfo);
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
			Set<String> toDo = this.getDirectoryContentManager().getProcessingBuffer().keySet();
			for (String fileName : toDo) {
				File file = new File(this.localWorkingDirectory, fileName);
				if (file.exists()) {
					file.delete();
				}
				FileOutputStream fileOutputStream = new FileOutputStream(file);
				client.retrieveFile(fileName, fileOutputStream);
				fileOutputStream.close();
				files.add(file);
			}
			return files;
		}
		finally {
			this.clientPool.releaseClient(client);
		}
	}

	@Override
	public void onSend(Message<?> message) {
		Object payload = message.getPayload();
		if (payload instanceof List) {
			List<?> items = (List<?>) payload;
			for (Object item : items) {
				if (item instanceof File) {
					fileProcessed(((File) item).getName());
				}
				else if (logger.isWarnEnabled()) {
					logger.warn("FtpSource.onSend() expects Files in the List payload, "
							+ "but received an item of type [" + item.getClass() + "]");
				}
			}
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("FtpSource.onSend() exepects a Message with a List of Files, "
					+ "but received payload of type [" + message.getPayload().getClass() + "].");
		}
	}

}
