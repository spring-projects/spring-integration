/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.sftp.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.core.NestedIOException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpATTRS;
import com.jcraft.jsch.SftpException;

/**
 * Default SFTP {@link Session} implementation. Wraps a JSCH session instance.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpSession implements Session<LsEntry> {

	private static final Log LOGGER = LogFactory.getLog(SftpSession.class);

	private static final String SESSION_IS_NOT_CONNECTED = "session is not connected";

	private static final Duration DEFAULT_CHANNEL_CONNECT_TIMEOUT = Duration.ofSeconds(5);

	private final com.jcraft.jsch.Session jschSession;

	private final JSchSessionWrapper wrapper;

	private int channelConnectTimeout = (int) DEFAULT_CHANNEL_CONNECT_TIMEOUT.toMillis();

	private volatile ChannelSftp channel;

	private volatile boolean closed;


	public SftpSession(com.jcraft.jsch.Session jschSession) {
		Assert.notNull(jschSession, "jschSession must not be null");
		this.jschSession = jschSession;
		this.wrapper = null;
	}

	public SftpSession(JSchSessionWrapper wrapper) {
		Assert.notNull(wrapper, "wrapper must not be null");
		this.jschSession = wrapper.getSession();
		this.wrapper = wrapper;
	}

	/**
	 * Set the connect timeout.
	 * @param timeout the timeout to set.
	 * @since 5.2
	 */
	public void setChannelConnectTimeout(Duration timeout) {
		Assert.notNull(timeout, "'timeout' cannot be null");
		this.channelConnectTimeout = (int) timeout.toMillis();
	}

	@Override
	public boolean remove(String path) throws IOException {
		Assert.state(this.channel != null, SESSION_IS_NOT_CONNECTED);
		try {
			this.channel.rm(path);
			return true;
		}
		catch (SftpException e) {
			throw new NestedIOException("Failed to remove file.", e);
		}
	}

	@Override
	public LsEntry[] list(String path) throws IOException {
		Assert.state(this.channel != null, SESSION_IS_NOT_CONNECTED);
		try {
			Vector<?> lsEntries = this.channel.ls(path); // NOSONAR (Vector)
			if (lsEntries != null) {
				LsEntry[] entries = new LsEntry[lsEntries.size()];
				for (int i = 0; i < lsEntries.size(); i++) {
					Object next = lsEntries.get(i);
					Assert.state(next instanceof LsEntry, "expected only LsEntry instances from channel.ls()");
					entries[i] = (LsEntry) next;
				}
				return entries;
			}
		}
		catch (SftpException e) {
			throw new NestedIOException("Failed to list files", e);
		}
		return new LsEntry[0];
	}

	@Override
	public String[] listNames(String path) throws IOException {
		LsEntry[] entries = this.list(path);
		List<String> names = new ArrayList<>();
		for (LsEntry entry : entries) {
			String fileName = entry.getFilename();
			SftpATTRS attrs = entry.getAttrs();
			if (!attrs.isDir() && !attrs.isLink()) {
				names.add(fileName);
			}
		}
		return names.toArray(new String[0]);
	}


	@Override
	public void read(String source, OutputStream os) throws IOException {
		Assert.state(this.channel != null, SESSION_IS_NOT_CONNECTED);
		try {
			InputStream is = this.channel.get(source);
			FileCopyUtils.copy(is, os);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to read file " + source, e);
		}
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		try {
			return this.channel.get(source);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to read file " + source, e);
		}
	}

	@Override
	public boolean finalizeRaw() {
		return true;
	}

	@Override
	public void write(InputStream inputStream, String destination) throws IOException {
		Assert.state(this.channel != null, SESSION_IS_NOT_CONNECTED);
		try {
			this.channel.put(inputStream, destination);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to write file", e);
		}
	}

	@Override
	public void append(InputStream inputStream, String destination) throws IOException {
		Assert.state(this.channel != null, SESSION_IS_NOT_CONNECTED);
		try {
			this.channel.put(inputStream, destination, ChannelSftp.APPEND);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to write file", e);
		}
	}

	@Override
	public void close() {
		this.closed = true;
		if (this.wrapper != null) {
			if (this.channel != null) {
				this.channel.disconnect();
			}
			this.wrapper.close();
		}
		else {
			if (this.jschSession.isConnected()) {
				this.jschSession.disconnect();
			}
		}
	}

	@Override
	public boolean isOpen() {
		return !this.closed && this.jschSession.isConnected();
	}

	@Override
	public void rename(String pathFrom, String pathTo) throws IOException {
		try {
			this.channel.rename(pathFrom, pathTo);
		}
		catch (SftpException sftpex) {
			if (LOGGER.isDebugEnabled()) {
				LOGGER.debug("Initial File rename failed, possibly because file already exists. " +
						"Will attempt to delete file: " + pathTo + " and execute rename again.");
			}
			try {
				remove(pathTo);
				if (LOGGER.isDebugEnabled()) {
					LOGGER.debug("Delete file: " + pathTo + " succeeded. Will attempt rename again");
				}
			}
			catch (IOException ioex) {
				NestedIOException exception = new NestedIOException("Failed to delete file " + pathTo, sftpex);
				exception.addSuppressed(ioex);
				throw exception; // NOSONAR - added to suppressed exceptions
			}
			try {
				// attempt to rename again
				this.channel.rename(pathFrom, pathTo);
			}
			catch (SftpException sftpex2) {
				NestedIOException exception =
						new NestedIOException("failed to rename from " + pathFrom + " to " + pathTo, sftpex);
				exception.addSuppressed(sftpex2);
				throw exception; // NOSONAR - added to suppressed exceptions
			}
		}
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("File: " + pathFrom + " was successfully renamed to " + pathTo);
		}
	}

	@Override
	public boolean mkdir(String remoteDirectory) throws IOException {
		try {
			this.channel.mkdir(remoteDirectory);
		}
		catch (SftpException ex) {
			if (ex.id != ChannelSftp.SSH_FX_FAILURE || !exists(remoteDirectory)) {
				throw new NestedIOException("failed to create remote directory '" + remoteDirectory + "'.", ex);
			}
		}
		return true;
	}

	@Override
	public boolean rmdir(String remoteDirectory) throws IOException {
		try {
			this.channel.rmdir(remoteDirectory);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to remove remote directory '" + remoteDirectory + "'.", e);
		}
		return true;
	}

	@Override
	public boolean exists(String path) {
		try {
			this.channel.lstat(path);
			return true;
		}
		catch (SftpException ex) {
			if (ex.id == ChannelSftp.SSH_FX_NO_SUCH_FILE) {
				return false;
			}
			else {
				throw new UncheckedIOException("Cannot check 'lstat' for path " + path,
						new IOException(ex));
			}
		}
	}

	void connect() {
		try {
			if (!this.jschSession.isConnected()) {
				this.jschSession.connect();
			}
			this.channel = (ChannelSftp) this.jschSession.openChannel("sftp");
			if (this.channel != null && !this.channel.isConnected()) {
				this.channel.connect(this.channelConnectTimeout);
			}
		}
		catch (JSchException e) {
			this.close();
			throw new IllegalStateException("failed to connect", e);
		}
	}

	@Override
	public ChannelSftp getClientInstance() {
		return this.channel;
	}

	@Override
	public String getHostPort() {
		return this.jschSession.getHost() + ':' + this.jschSession.getPort();
	}

	@Override
	public boolean test() {
		return isOpen() && doTest();
	}

	private boolean doTest() {
		try {
			this.channel.lstat(this.channel.getHome());
			return true;
		}
		catch (@SuppressWarnings("unused") Exception e) {
			return false;
		}
	}


}
