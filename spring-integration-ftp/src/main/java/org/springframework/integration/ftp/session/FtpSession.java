/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.ftp.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;

/**
 * Implementation of {@link Session} for FTP.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class FtpSession implements Session<FTPFile> {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final FTPClient client;


	public FtpSession(FTPClient client) {
		Assert.notNull(client, "client must not be null");
		this.client = client;
	}


	public boolean remove(String path) throws IOException {
		Assert.hasText(path, "path must not be null");
		boolean completed = this.client.deleteFile(path);
	 	if (!completed) {
			throw new IOException("Failed to delete '" + path + "'. Server replied with: " + client.getReplyString());
		}
		return completed;
	}
	
	public FTPFile[] list(String path) throws IOException {
		Assert.hasText(path, "path must not be null");
		return this.client.listFiles(path);
	}

	public void read(String path, OutputStream fos) throws IOException {
		Assert.hasText(path, "path must not be null");
		Assert.notNull(fos, "outputStream must not be null");
		boolean completed = this.client.retrieveFile(path, fos);
		if (!completed) {
			throw new IOException("Failed to copy '" + path +
					"'. Server replied with: " + this.client.getReplyString());
		}
		logger.info("File have been successfully transfered to: " + path);
	}

	public void write(InputStream inputStream, String path) throws IOException {
		Assert.notNull(inputStream, "inputStream must not be null");
		Assert.hasText(path, "path must not be null");
		boolean completed = this.client.storeFile(path, inputStream);
		if (!completed) {
			throw new IOException("Failed to write to '" + path 
					+ "'. Server replied with: " + this.client.getReplyString());
		}
		if (logger.isInfoEnabled()) {
			logger.info("File has been successfully transfered to: " + path);
		}
	}

	public void close() {
		try {
			this.client.disconnect();
		}
		catch (Exception e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to disconnect FTPClient", e);
			}
		}
	}

	public boolean isOpen() {
		try {
			this.client.noop();
		}
		catch (Exception e) {
			return false;
		}
		return true;
	}

	public void rename(String pathFrom, String pathTo) throws IOException{
		this.client.deleteFile(pathTo);
		boolean completed = this.client.rename(pathFrom, pathTo);
		if (!completed) {
			throw new IOException("Failed to rename '" + pathFrom + 
					"' to " + pathTo + "'. Server replied with: " + this.client.getReplyString());
		}
		if (logger.isInfoEnabled()) {
			logger.info("File has been successfully renamed from: " + pathFrom + " to " + pathTo);
		}
	}
	
	public void mkdir(String remoteDirectory) throws IOException {
		this.client.makeDirectory(remoteDirectory);
	}
	
	public boolean exists(String path) throws IOException{
		Assert.hasText(path, "'path' must not be empty");
		String currentWorkingPath = this.client.printWorkingDirectory();
		boolean exists = false;
		if (currentWorkingPath != null){
			try {
				if (this.client.changeWorkingDirectory(path)){	
					exists = true;
				}
			} 
			finally {
				this.client.changeWorkingDirectory(currentWorkingPath);
			}
		}
			
		return exists;
	}
}
