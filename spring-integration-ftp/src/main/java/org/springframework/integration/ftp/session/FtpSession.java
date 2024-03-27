/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.ftp.FTPReply;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Implementation of {@link Session} for FTP.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Den Ivanov
 *
 * @since 2.0
 */
public class FtpSession implements Session<FTPFile> {

	private static final Log LOGGER = LogFactory.getLog(FtpSession.class);

	private static final String SERVER_REPLIED_WITH = "'. Server replied with: ";

	private final FTPClient client;

	private final AtomicBoolean readingRaw = new AtomicBoolean();

	public FtpSession(FTPClient client) {
		Assert.notNull(client, "client must not be null");
		this.client = client;
	}

	@Override
	public boolean remove(String path) throws IOException {
		Assert.hasText(path, "path must not be null");
		if (!this.client.deleteFile(path)) {
			throw new IOException("Failed to delete '" + path + SERVER_REPLIED_WITH + this.client.getReplyString());
		}
		else {
			return true;
		}
	}

	@Override
	public FTPFile[] list(String path) throws IOException {
		return this.client.listFiles(path);
	}

	@Override
	public String[] listNames(String path) throws IOException {
		return this.client.listNames(path);
	}

	@Override
	public void read(String path, OutputStream fos) throws IOException {
		Assert.hasText(path, "path must not be null");
		Assert.notNull(fos, "outputStream must not be null");
		boolean completed = this.client.retrieveFile(path, fos);
		if (!completed) {
			throw new IOException("Failed to copy '" + path +
					SERVER_REPLIED_WITH + this.client.getReplyString());
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("File has been successfully transferred from: " + path);
		}
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		if (!this.readingRaw.compareAndSet(false, true)) {
			throw new IOException("Previous raw read was not finalized");
		}
		InputStream inputStream = this.client.retrieveFileStream(source);
		if (inputStream == null) {
			throw new IOException("Failed to obtain InputStream for remote file " + source + ": "
					+ this.client.getReplyCode());
		}
		return inputStream;
	}

	@Override
	public boolean finalizeRaw() throws IOException {
		if (!this.readingRaw.compareAndSet(true, false)) {
			throw new IOException("Raw read is not in process");
		}
		if (FTPReply.isNegativePermanent(this.client.getReplyCode())) {
			// The 'readRaw()' has failed - nothing to complete.
			return true;
		}
		if (this.client.completePendingCommand()) {
			int replyCode = this.client.getReplyCode();
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug(this + " finalizeRaw - reply code: " + replyCode);
			}
			return FTPReply.isPositiveCompletion(replyCode);
		}
		throw new IOException("completePendingCommandFailed");
	}

	@Override
	public void write(InputStream inputStream, String path) throws IOException {
		Assert.notNull(inputStream, "inputStream must not be null");
		Assert.hasText(path, "path must not be null or empty");
		boolean completed = this.client.storeFile(path, inputStream);
		if (!completed) {
			throw new IOException("Failed to write to '" + path
					+ SERVER_REPLIED_WITH + this.client.getReplyString());
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("File has been successfully transferred to: " + path);
		}
	}

	@Override
	public void append(InputStream inputStream, String path) throws IOException {
		Assert.notNull(inputStream, "inputStream must not be null");
		Assert.hasText(path, "path must not be null or empty");
		boolean completed = this.client.appendFile(path, inputStream);
		if (!completed) {
			throw new IOException("Failed to append to '" + path
					+ SERVER_REPLIED_WITH + this.client.getReplyString());
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("File has been successfully appended to: " + path);
		}
	}

	@Override
	public void close() {
		try {
			if (this.readingRaw.get() && !finalizeRaw() && LOGGER.isDebugEnabled()) {
				LOGGER.debug("Finalize on readRaw() returned false for " + this);
			}
			if (isOpen()) {
				try {
					this.client.logout();
				}
				catch (IOException ex) {
					LOGGER.debug("failed to logout FTPClient", ex);
				}
			}
			this.client.disconnect();
		}
		catch (Exception ex) {
			LOGGER.debug("failed to disconnect FTPClient", ex);
		}
	}

	@Override
	public boolean isOpen() {
		try {
			this.client.noop();
		}
		catch (Exception ex) {
			LOGGER.debug("failed to noop FTPClient", ex);
			return false;
		}
		return true;
	}

	@Override
	public void rename(String pathFrom, String pathTo) throws IOException {
		this.client.deleteFile(pathTo);
		boolean completed = this.client.rename(pathFrom, pathTo);
		if (!completed) {
			throw new IOException("Failed to rename " + pathFrom +
					" to " + pathTo + SERVER_REPLIED_WITH + this.client.getReplyString());
		}
		if (LOGGER.isInfoEnabled()) {
			LOGGER.info("File has been successfully renamed from " + pathFrom + " to " + pathTo);
		}
	}

	@Override
	public boolean mkdir(String remoteDirectory) throws IOException {
		return this.client.makeDirectory(remoteDirectory);
	}

	@Override
	public boolean rmdir(String directory) throws IOException {
		return this.client.removeDirectory(directory);
	}

	@Override
	public boolean exists(String path) throws IOException {
		Assert.hasText(path, "'path' must not be empty");

		String[] names = this.client.listNames(path);
		boolean exists = !ObjectUtils.isEmpty(names);

		if (!exists) {
			String currentWorkingPath = this.client.printWorkingDirectory();
			Assert.state(currentWorkingPath != null,
					"working directory cannot be determined; exists check can not be completed");

			try {
				exists = this.client.changeWorkingDirectory(path);
			}
			finally {
				this.client.changeWorkingDirectory(currentWorkingPath);
			}

		}

		return exists;
	}

	@Override
	public FTPClient getClientInstance() {
		return this.client;
	}

	@Override
	public String getHostPort() {
		return this.client.getRemoteAddress().getHostName() + ':' + this.client.getRemotePort();
	}

	@Override
	public boolean test() {
		return isOpen() && doTest();
	}

	private boolean doTest() {
		try {
			this.client.noop();
			return true;
		}
		catch (IOException e) {
			return false;
		}
	}

}
