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


	public void connect() {
	}

	public void disconnect() {
		try {
			this.client.disconnect();
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to disconnect FTPClient", e);
			}
		}
	}

	public boolean exists(String path) {
		return false;
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
			return this.client.retrieveFileStream(source);
		}
		catch (IOException e) {
			if (logger.isWarnEnabled()) {
				logger.warn("failed to disconnect FTPClient", e);
			}
			return null;
		}
	}

	public void put(InputStream inputStream, String destination) {
		try {
			// TODO:
			// String originalDirectory = this.client.printWorkingDirectory()
			//  tokenize destination into 'directory' and 'file'
			//       then changeWorkingDirectory(directory)
			this.client.storeFile(destination, inputStream);
		}
		catch (IOException e) {
			throw new IllegalStateException("failed to copy file", e);
		}
	}

}
