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

package org.springframework.integration.ftp.session;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class FtpSession implements Session {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final FTPClient client;


	public FtpSession(FTPClient client) {
		Assert.notNull(client, "client must not be null");
		this.client = client;
	}


	public boolean rm(String path) {
		try {
			this.client.deleteFile(path);
			return true;
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to delete file", e);
			}
			return false;
		}
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public <F> Collection<F> ls(String path) {
		try {
			FTPFile[] files = this.client.listFiles(path);
			ArrayList list = new ArrayList();
			for (FTPFile file : files) {
				list.add(file);
			}
			return list;
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to list files", e);
			}
			return Collections.EMPTY_LIST;
		}
	}

	public InputStream get(String source) {
		try {
			InputStream inputStream = this.client.retrieveFileStream(source);
			this.client.completePendingCommand();
			return inputStream;
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to disconnect FTPClient", e);
			}
			return null;
		}
	}

	public void put(InputStream inputStream, String destination) {
		String originalWorkDir = null;
		try {
			String fileName = StringUtils.getFilename(destination);
			int startOfFileName = destination.lastIndexOf(File.separatorChar);
			
			if (startOfFileName > 0) {
				originalWorkDir = client.printWorkingDirectory();
				String pathname = destination.substring(0, startOfFileName);
				client.changeWorkingDirectory(pathname);
			}	
			this.client.storeFile(fileName, inputStream);
		}
		catch (IOException e) {
			throw new IllegalStateException("failed to copy file", e);
		}
		finally {
			if (originalWorkDir != null){
				try {
					this.client.changeWorkingDirectory(originalWorkDir);
				} catch (IOException ioex) {
					throw new IllegalStateException("failed to change working directories ", ioex);
				}	
			}	
		}
	}

	public void close() {
		try {
			this.client.disconnect();
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to disconnect FTPClient", e);
			}
		}
	}

}
