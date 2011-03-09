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

package org.springframework.integration.sftp.session;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Vector;

import org.springframework.core.NestedIOException;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.JSchException;
import com.jcraft.jsch.SftpException;

/**
 * Default SFTP {@link Session} implementation. Wraps a JSCH session instance.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class SftpSession implements Session {

	private volatile ChannelSftp channel;

	private final com.jcraft.jsch.Session jschSession;


	public SftpSession(com.jcraft.jsch.Session jschSession) {
		Assert.notNull(jschSession, "jschSession must not be null");
		this.jschSession = jschSession;
	}


	public boolean remove(String path) throws IOException{
		Assert.state(this.channel != null, "session is not connected");
		try {
			this.channel.rm(path);
			return true;
		}
		catch (SftpException e) {
			throw new NestedIOException("Failed to remove file: "+ e);
		}
	}

	@SuppressWarnings("unchecked")
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

	public void read(String source, OutputStream os) throws IOException{
		Assert.state(this.channel != null, "session is not connected");
		
		try {
			InputStream is = this.channel.get(source);
			FileCopyUtils.copy(is, os);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to read file", e);
		}
	}

	public void write(InputStream inputStream, String destination) throws IOException{
		Assert.state(this.channel != null, "session is not connected");
		try {
			this.channel.put(inputStream, destination);
		}
		catch (SftpException e) {
			throw new NestedIOException("failed to write file", e);
		}
	}

	public void close() {
		if (this.jschSession.isConnected()) {
			this.jschSession.disconnect();
		}
	}

	void connect() {
		try {
			if (!this.jschSession.isConnected()) {
				this.jschSession.connect();
				this.channel = (ChannelSftp) this.jschSession.openChannel("sftp");
			}
			if (this.channel != null && !this.channel.isConnected()) {
				this.channel.connect();
			}
		}
		catch (JSchException e) {
			throw new IllegalStateException("failed to connect", e);
		}
	}

	public boolean isOpen() {
		return this.jschSession.isConnected();
	}

	public void rename(String pathFrom, String pathTo) throws IOException {
		try {
			this.channel.rename(pathFrom, pathTo);
		} catch (SftpException e) {
			throw new NestedIOException("failed to rename from " + pathFrom + " to " + pathTo, e);
		}
	}

	public void mkdir(String directory) throws IOException {
		try {
			this.channel.mkdir(directory);
		} catch (SftpException e) {
			throw new NestedIOException("failed to create remote directory '" + directory + "'.", e);
		}
	}

}
