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

package org.springframework.integration.sftp.session;

import java.nio.file.attribute.PosixFilePermissions;

import org.apache.sshd.sftp.client.SftpClient;
import org.apache.sshd.sftp.common.SftpHelper;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.integration.file.remote.FileInfo} implementation for SFTP.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class SftpFileInfo extends AbstractFileInfo<SftpClient.DirEntry> {

	private final SftpClient.DirEntry lsEntry;

	private final SftpClient.Attributes attrs;

	public SftpFileInfo(SftpClient.DirEntry lsEntry) {
		Assert.notNull(lsEntry, "'lsEntry' must not be null");
		this.lsEntry = lsEntry;
		this.attrs = lsEntry.getAttributes();
	}

	/**
	 * @see SftpClient.Attributes#isDirectory()
	 */
	@Override
	public boolean isDirectory() {
		return this.attrs.isDirectory();
	}

	/**
	 * @see SftpClient.Attributes#isSymbolicLink()
	 */
	@Override
	public boolean isLink() {
		return this.attrs.isSymbolicLink();
	}

	/**
	 * @see SftpClient.Attributes#getSize()
	 */
	@Override
	public long getSize() {
		return this.attrs.getSize();
	}

	/**
	 * @see SftpClient.Attributes#getModifyTime()
	 */
	@Override
	public long getModified() {
		return this.attrs.getModifyTime().toMillis();
	}

	/**
	 * @see SftpClient.DirEntry#getFilename()
	 */
	@Override
	public String getFilename() {
		return this.lsEntry.getFilename();
	}

	@Override
	public String getPermissions() {
		return PosixFilePermissions.toString(SftpHelper.permissionsToAttributes(this.attrs.getPermissions()));
	}

	@Override
	public SftpClient.DirEntry getFileInfo() {
		return this.lsEntry;
	}

}
