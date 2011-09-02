/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.util.Assert;

import com.jcraft.jsch.ChannelSftp.LsEntry;
import com.jcraft.jsch.SftpATTRS;

/**
 * A {@link FileInfo} implementation for SFTP.
 * 
 * @author Gary Russell
 * @since 2.1
 */
public class SftpFileInfo extends AbstractFileInfo<LsEntry> {

	private final LsEntry lsEntry;

	private final SftpATTRS attrs;


	public SftpFileInfo(LsEntry lsEntry) {
		Assert.notNull("LsEntry must not be null");
		this.lsEntry = lsEntry;
		this.attrs = lsEntry.getAttrs();
	}

	/**
	 * @return
	 * @see com.jcraft.jsch.SftpATTRS#isDir()
	 */
	public boolean isDirectory() {
		return this.attrs.isDir();
	}

	/**
	 * @return
	 * @see com.jcraft.jsch.SftpATTRS#isLink()
	 */
	public boolean isLink() {
		return this.attrs.isLink();
	}

	/**
	 * @return
	 * @see com.jcraft.jsch.SftpATTRS#getSize()
	 */
	public long getSize() {
		return this.attrs.getSize();
	}

	/**
	 * @return
	 * @see com.jcraft.jsch.SftpATTRS#getMTime()
	 */
	public long getModified() {
		return this.attrs.getMTime() * 1000;
	}

	/**
	 * @return
	 * @see com.jcraft.jsch.ChannelSftp.LsEntry#getFilename()
	 */
	public String getFilename() {
		return this.lsEntry.getFilename();
	}

	public String getPermissions() {
		return this.attrs.getPermissionsString();
	}

	public LsEntry getFileInfo() {
		return this.lsEntry;
	}

}
