/*
 * Copyright 2002-2025 the original author or authors.
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
import java.net.SocketAddress;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.apache.sshd.client.session.ClientSession;
import org.apache.sshd.common.util.net.SshdSocketAddress;
import org.apache.sshd.sftp.SftpModuleProperties;
import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.common.SftpConstants;
import org.apache.sshd.sftp.common.SftpException;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.PatternMatchUtils;
import org.springframework.util.StringUtils;

/**
 * Default SFTP {@link Session} implementation. Wraps a MINA SSHD session instance.
 *
 * @author Josh Long
 * @author Mario Gray
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 * @author Darryl Smith
 * @since 2.0
 */
public class SftpSession implements Session<SftpClient.DirEntry> {

	private final SftpClient sftpClient;

	public SftpSession(SftpClient sftpClient) {
		Assert.notNull(sftpClient, "'sftpClient' must not be null");
		this.sftpClient = sftpClient;
	}

	@Override
	public boolean remove(String path) throws IOException {
		try {
			this.sftpClient.remove(path);
		}
		catch (SftpException sftpEx) {
			if (SftpConstants.SSH_FX_NO_SUCH_FILE == sftpEx.getStatus()) {
				return false;
			}
			else {
				throw sftpEx;
			}
		}

		return true;
	}

	@Override
	public SftpClient.DirEntry[] list(String path) throws IOException {
		return doList(path)
				.toArray(SftpClient.DirEntry[]::new);
	}

	@Override
	public String[] listNames(String path) throws IOException {
		return doList(path)
				.map(SftpClient.DirEntry::getFilename)
				.toArray(String[]::new);
	}

	public Stream<SftpClient.DirEntry> doList(String path) throws IOException {
		String remotePath = StringUtils.trimTrailingCharacter(path, '/');
		String remoteDir = remotePath;
		int lastIndex = remotePath.lastIndexOf('/');
		if (lastIndex > 0) {
			remoteDir = remoteDir.substring(0, lastIndex);
		}
		String remoteFile = lastIndex > 0 ? remotePath.substring(lastIndex + 1) : null;
		boolean isPattern = remoteFile != null && remoteFile.contains("*");

		if (!isPattern && remoteFile != null) {
			SftpClient.Attributes attributes = this.sftpClient.stat(path);
			if (!attributes.isDirectory()) {
				return Stream.of(new SftpClient.DirEntry(remoteFile, path, attributes));
			}
			else {
				remoteDir = remotePath;
			}
		}
		remoteDir = normalizePath(remoteDir);
		return StreamSupport.stream(this.sftpClient.readDir(remoteDir).spliterator(), false)
				.filter((entry) -> !isPattern || PatternMatchUtils.simpleMatch(remoteFile, entry.getFilename()));
	}

	@Override
	public void read(String source, OutputStream os) throws IOException {
		InputStream is = readRaw(source);
		FileCopyUtils.copy(is, os);
	}

	@Override
	public InputStream readRaw(String source) throws IOException {
		return this.sftpClient.read(normalizePath(source));
	}

	private String normalizePath(String path) throws IOException {
		return !path.isEmpty() && path.charAt(0) == '/' ? path : this.sftpClient.canonicalPath(path);
	}

	@Override
	public boolean finalizeRaw() {
		return true;
	}

	@Override
	public void write(InputStream inputStream, String destination) throws IOException {
		OutputStream outputStream = this.sftpClient.write(destination);
		FileCopyUtils.copy(inputStream, outputStream);
	}

	@Override
	public void append(InputStream inputStream, String destination) throws IOException {
		OutputStream outputStream =
				this.sftpClient.write(destination,
						SftpClient.OpenMode.Create,
						SftpClient.OpenMode.Write,
						SftpClient.OpenMode.Append);
		FileCopyUtils.copy(inputStream, outputStream);
	}

	@Override
	public void close() {
		try {
			this.sftpClient.close();
		}
		catch (IOException ex) {
			throw new UncheckedIOException("failed to close an SFTP client", ex);
		}

		try {
			ClientSession session = this.sftpClient.getSession();
			if (session != null && session.isOpen()) {
				session.close();
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("failed to close an SFTP client (session)", ex);
		}
	}

	@Override
	public boolean isOpen() {
		return this.sftpClient.isOpen();
	}

	@Override
	public void rename(String pathFrom, String pathTo) throws IOException {
		if (this.sftpClient.getVersion() >= SftpConstants.SFTP_V5) {
			this.sftpClient.rename(pathFrom, pathTo, SftpClient.CopyMode.Overwrite);
		}
		else {
			remove(pathTo);
			this.sftpClient.rename(pathFrom, pathTo);
		}
	}

	@Override
	public boolean mkdir(String remoteDirectory) throws IOException {
		this.sftpClient.mkdir(remoteDirectory);
		return true;
	}

	@Override
	public boolean rmdir(String remoteDirectory) throws IOException {
		this.sftpClient.rmdir(remoteDirectory);
		return true;
	}

	@Override
	public boolean exists(String path) {
		try {
			this.sftpClient.stat(normalizePath(path));
			return true;
		}
		catch (SftpException ex) {
			if (SftpConstants.SSH_FX_NO_SUCH_FILE == ex.getStatus()) {
				return false;
			}
			else {
				throw new UncheckedIOException("Cannot check 'lstat' for path " + path, ex);
			}
		}
		catch (IOException ex) {
			throw new UncheckedIOException("Cannot check 'lstat' for path " + path, ex);
		}
	}

	void connect() {
		try {
			if (!this.sftpClient.isOpen()) {
				Duration initializationTimeout =
						SftpModuleProperties.SFTP_CHANNEL_OPEN_TIMEOUT.getRequired(this.sftpClient.getSession());
				this.sftpClient.getClientChannel().open().verify(initializationTimeout);
			}
		}
		catch (IOException ex) {
			close();
			throw new UncheckedIOException("failed to connect an SFTP client", ex);
		}
	}

	@Override
	public SftpClient getClientInstance() {
		return this.sftpClient;
	}

	@Override
	public String getHostPort() {
		SocketAddress connectAddress = this.sftpClient.getSession().getConnectAddress();
		return SshdSocketAddress.toAddressString(connectAddress) + ':' + SshdSocketAddress.toAddressPort(connectAddress);
	}

	@Override
	public boolean test() {
		return isOpen() && doTest();
	}

	private boolean doTest() {
		try {
			this.sftpClient.canonicalPath("");
			return true;
		}
		catch (@SuppressWarnings("unused") Exception ex) {
			return false;
		}
	}

}
