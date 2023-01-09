/*
 * Copyright 2002-2023 the original author or authors.
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

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;

/**
 * A {@link org.springframework.integration.file.remote.FileInfo} implementation for SFTP.
 *
 * @author Gary Russell
 * @since 2.1
 */
public class SftpFileInfo extends AbstractFileInfo<LsEntry> {

	private final LsEntry lsEntry;

	private final SftpATTRS attrs;


	public SftpFileInfo(LsEntry lsEntry) {
		Assert.notNull(lsEntry, "'lsEntry' must not be null");
		this.lsEntry = lsEntry;
		this.attrs = lsEntry.getAttrs();
	}

	/**
	 * @see com.jcraft.jsch.SftpATTRS#isDir()
	 */
	@Override
	public boolean isDirectory() {
		return this.attrs.isDir();
	}

	/**
	 * @see com.jcraft.jsch.SftpATTRS#isLink()
	 */
	@Override
	public boolean isLink() {
		return this.attrs.isLink();
	}

	/**
	 * @see com.jcraft.jsch.SftpATTRS#getSize()
	 */
	@Override
	public long getSize() {
		return this.attrs.getSize();
	}

	/**
	 * @see com.jcraft.jsch.SftpATTRS#getMTime()
	 */
	@Override
	public long getModified() {
		return ((long) this.attrs.getMTime()) * 1000; // NOSONAR magic number
	}

	/**
	 * @see com.jcraft.jsch.ChannelSftp.LsEntry#getFilename()
	 */
	@Override
	public String getFilename() {
		return this.lsEntry.getFilename();
	}

	@Override
	public String getPermissions() {
		return this.attrs.getPermissionsString();
	}

	@Override
	public LsEntry getFileInfo() {
		return this.lsEntry;
	}

}
