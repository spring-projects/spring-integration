/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.sftp.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
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
 * @since 2.0
 */
public class SftpSession implements Session<LsEntry> {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final com.jcraft.jsch.Session jschSession;

	private final JSchSessionWrapper wrapper;

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

	@Override
	public boolean remove(String path) throws IOException {
		Assert.state(this.channel != null, "session is not connected");
		try {
			this.channel.rm(path);
			return true;
		}
		catch (SftpException e) {
			throw new NestedIOException("Failed to remove file: "+ e);
		}
	}

	@Override
	public LsEntry[] list(String path) throws IOException {
		Assert.state(this.channel != null, "session is not connected");
		try {
			Vector<?> lsEntries = this.channel.ls(path);
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
		List<String> names = new ArrayList<String>();
		for (int i = 0; i < entries.length; i++) {
			String fileName = entries[i].getFilename();
			SftpATTRS attrs = entries[i].getAttrs();
			if (!attrs.isDir() && !attrs.isLink()) {
				names.add(fileName);
			}
		}
		String[] fileNames = new String[names.size()];
		return names.toArray(fileNames);
	}


	@Override
	public void read(String source, OutputStream os) throws IOException {
		Assert.state(this.channel != null, "session is not connected");
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
	public boolean finalizeRaw() throws IOException {
		return true;
	}

	@Override
	public void write(InputStream inputStream, String destination) throws IOException {
		Assert.state(this.channel != null, "session is not connected");
		try {
			this.channel.put(inputStream, destination);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to write file", e);
		}
	}

	@Override
	public void append(InputStream inputStream, String destination) throws IOException {
		Assert.state(this.channel != null, "session is not connected");
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
			if (logger.isDebugEnabled()){
				logger.debug("Initial File rename failed, possibly because file already exists. Will attempt to delete file: "
						+ pathTo + " and execute rename again.");
			}
			try {
				this.remove(pathTo);
				if (logger.isDebugEnabled()) {
					logger.debug("Delete file: " + pathTo + " succeeded. Will attempt rename again");
				}
			}
			catch (IOException ioex) {
				throw new NestedIOException("Failed to delete file " + pathTo, ioex);
			}
			try {
				// attempt to rename again
				this.channel.rename(pathFrom, pathTo);
			}
			catch (SftpException sftpex2) {
				throw new NestedIOException("failed to rename from " + pathFrom + " to " + pathTo, sftpex2);
			}
		}
		if (logger.isDebugEnabled()) {
			logger.debug("File: " + pathFrom + " was successfully renamed to " + pathTo);
		}
	}

	@Override
	public boolean mkdir(String remoteDirectory) throws IOException {
		try {
			this.channel.mkdir(remoteDirectory);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to create remote directory '" + remoteDirectory + "'.", e);
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
		catch (SftpException e) {
			// ignore
		}
		return false;
	}

	void connect() {
		try {
			if (!this.jschSession.isConnected()) {
				this.jschSession.connect();
			}
			this.channel = (ChannelSftp) this.jschSession.openChannel("sftp");
			if (this.channel != null && !this.channel.isConnected()) {
				this.channel.connect();
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

}
