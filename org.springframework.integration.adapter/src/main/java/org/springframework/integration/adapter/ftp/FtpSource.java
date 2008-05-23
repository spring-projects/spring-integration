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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.adapter.file.ByteArrayFileMessageCreator;
import org.springframework.integration.adapter.file.FileNameGenerator;
import org.springframework.integration.adapter.file.TextFileMessageCreator;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessageDeliveryAware;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.Source;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A source adapter for receiving files via FTP.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class FtpSource implements Source<Object>, MessageDeliveryAware {

	private final static String DEFAULT_HOST = "localhost";

	private final static int DEFAULT_PORT = 21;

	private final static String DEFAULT_REMOTE_WORKING_DIRECTORY = "/";


	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String username;

	private volatile String password;

	private volatile String host = DEFAULT_HOST;

	private volatile int port = DEFAULT_PORT;

	private volatile String remoteWorkingDirectory = DEFAULT_REMOTE_WORKING_DIRECTORY;

	private volatile File localWorkingDirectory;

	private volatile boolean textBased = true;

	private volatile MessageCreator<File, ?> messageCreator;

	private final DirectoryContentManager directoryContentManager = new DirectoryContentManager();

	private final FTPClient client = new FTPClient();


	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRemoteWorkingDirectory(String remoteWorkingDirectory) {
		Assert.hasText(remoteWorkingDirectory, "'remoteWorkingDirectory' is required");
		this.remoteWorkingDirectory = remoteWorkingDirectory;
	}

	public void setLocalWorkingDirectory(File localWorkingDirectory) {
		Assert.notNull(localWorkingDirectory, "'localWorkingDirectory' must not be null");
		this.localWorkingDirectory = localWorkingDirectory;
	}

	public boolean isTextBased() {
		return this.textBased;
	}

	public void setTextBased(boolean textBased) {
		this.textBased = textBased;
	}

	public void afterPropertiesSet() {
		if (this.isTextBased()) {
			this.messageCreator = new TextFileMessageCreator();
		}
		else {
			this.messageCreator = new ByteArrayFileMessageCreator();
		}
	}

	public final Message receive() {
		try {
			this.establishConnection();
			FTPFile[] fileList = this.client.listFiles();
			HashMap<String, FileInfo> snapshot = new HashMap<String, FileInfo>();
			for (FTPFile ftpFile : fileList) {
				FileInfo fileInfo = new FileInfo(
						ftpFile.getName(), ftpFile.getTimestamp().getTimeInMillis(), ftpFile.getSize());
				snapshot.put(ftpFile.getName(), fileInfo);
			}
			this.directoryContentManager.processSnapshot(snapshot);
			Map<String, FileInfo> backlog = this.directoryContentManager.getBacklog();
			if (backlog.isEmpty()) {
				return null;
			}
			String fileName = backlog.keySet().iterator().next();
			File file = new File(this.localWorkingDirectory, fileName);
			if (file.exists()) {
				file.delete();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			this.client.retrieveFile(fileName, fileOutputStream);
			fileOutputStream.close();
			return this.messageCreator.createMessage(file);
		}
		catch (Exception e) {
			try {
				if (this.client.isConnected()) {
					this.client.disconnect();
				}
			}
			catch (IOException ioe) {
				throw new MessagingException("Error when disconnecting from ftp.", ioe);
			}
			throw new MessagingException("Error while polling for messages.", e);
		}
	}

	private void establishConnection() throws IOException {
		if (!StringUtils.hasText(this.username)) {
			throw new MessagingException("username is required");
		}
		this.client.connect(this.host, this.port);
		if (!this.client.login(this.username, this.password)) {
			throw new MessagingException("Login failed. Please check the username and password.");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("login successful");
		}
		this.client.setFileType(FTP.IMAGE_FILE_TYPE);
		if (!this.remoteWorkingDirectory.equals(this.client.printWorkingDirectory())
				&& !this.client.changeWorkingDirectory(this.remoteWorkingDirectory)) {
				throw new MessagingException("Could not change directory to '" +
						remoteWorkingDirectory + "'. Please check the path.");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("working directory is: " + this.client.printWorkingDirectory());
		}
	}

	public void onSend(Message<?> message) {
		String filename = message.getHeader().getProperty(FileNameGenerator.FILENAME_PROPERTY_KEY);
		if (StringUtils.hasText(filename)) {
			this.directoryContentManager.fileProcessed(filename);
		}
		else if (this.logger.isWarnEnabled()) {
			logger.warn("No filename in Message header, cannot send notification of processing.");
		}
	}

	public void onFailure(MessagingException exception) {
		if (this.logger.isWarnEnabled()) {
			logger.warn("FtpSource received failure notification", exception);
		}
	}

}
