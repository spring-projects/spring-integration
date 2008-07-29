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
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Required;
import org.springframework.integration.adapter.file.AbstractDirectorySource;
import org.springframework.integration.adapter.file.FileInfo;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageCreator;
import org.springframework.integration.message.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * A source adapter for receiving files via FTP.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class FtpSource extends AbstractDirectorySource<List<File>> implements DisposableBean {

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

	private final FTPClient client;

	public FtpSource(MessageCreator<List<File>, List<File>> messageCreator) {
		this(messageCreator, new FTPClient());
	}

	public FtpSource(MessageCreator<List<File>, List<File>> messageCreator, FTPClient client) {
		super(messageCreator);
		this.client = client;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Required
	public void setUsername(String username) {
		this.username = username;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setRemoteWorkingDirectory(String remoteWorkingDirectory) {
		Assert.notNull(remoteWorkingDirectory, "'remoteWorkingDirectory' cannot be null");
		// FtpClient is picky about "", so we make it happy
		this.remoteWorkingDirectory = remoteWorkingDirectory.replaceAll("^$", "/");
	}

	public void setLocalWorkingDirectory(File localWorkingDirectory) {
		Assert.notNull(localWorkingDirectory, "'localWorkingDirectory' must not be null");
		this.localWorkingDirectory = localWorkingDirectory;
	}

	@Override
	protected void populateSnapshot(Map<String, FileInfo> snapshot) throws IOException {
		establishConnection();
		FTPFile[] fileList = this.client.listFiles();

		for (FTPFile ftpFile : fileList) {
			FileInfo fileInfo = new FileInfo(ftpFile.getName(), ftpFile.getTimestamp().getTimeInMillis(), ftpFile
					.getSize());
			snapshot.put(ftpFile.getName(), fileInfo);
		}
	}

	protected void establishConnection() throws IOException {
		if (this.client.isConnected()) {
			if (logger.isDebugEnabled()) {
				logger.debug("client already connected");
			}
			return;
		}
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
			throw new MessagingException("Could not change directory to '" + remoteWorkingDirectory
					+ "'. Please check the path.");
		}
		if (logger.isDebugEnabled()) {
			logger.debug("working directory is: " + this.client.printWorkingDirectory());
		}
	}

	protected List<File> retrieveNextPayload() throws IOException {
		establishConnection();
		List<File> files = new ArrayList<File>();
		Set<String> backlog = this.getDirectoryContentManager().getBacklog().keySet();
		for (String fileName : backlog) {
			File file = new File(this.localWorkingDirectory, fileName);
			if (file.exists()) {
				file.delete();
			}
			FileOutputStream fileOutputStream = new FileOutputStream(file);
			this.client.retrieveFile(fileName, fileOutputStream);
			fileOutputStream.close();
			files.add(file);
		}
		disconnect();
		return files;
	}

	protected void disconnect() {
		try {
			if (this.client.isConnected()) {
				this.client.disconnect();
				if (logger.isDebugEnabled()) {
					logger.debug("connection closed");
				}
			}
		}
		catch (IOException ioe) {
			if (logger.isErrorEnabled()) {
				logger.error("Error when disconnecting from ftp.", ioe);
			}
		}
	}

	public void destroy() throws Exception {
		disconnect();
	}

	@Override
	public void onSend(Message<?> message) {
		List<File> files = ((Message<List<File>>) message).getPayload();
		for (File file : files) {
			fileProcessed(file.getName());
		}
	}

}
