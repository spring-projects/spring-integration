/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.smb.session;

import java.io.IOException;
import java.util.Arrays;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.util.Assert;

import jcifs.internal.dtyp.ACE;
import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;

/**
 * A {@link org.springframework.integration.file.remote.FileInfo} implementation for
 * SMB.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbFileInfo extends AbstractFileInfo<SmbFile> {

	private static final Log logger = LogFactory.getLog(SmbFileInfo.class);

	private final SmbFile smbFile;

	public SmbFileInfo(SmbFile smbFile) {
		Assert.notNull(smbFile, "SmbFile must not be null");
		this.smbFile = smbFile;
	}

	@Override
	public boolean isDirectory() {
		try {
			return this.smbFile.isDirectory();
		}
		catch (SmbException se) {
			logger.error("Unable to determine if this SmbFile represents a directory", se);
			return false;
		}
	}

	/**
	 * Symbolic links are currently not supported in the JCIFS v2.x.x
	 * dependent library, so this method will always return false.
	 * @return false
	 */
	@Override
	public boolean isLink() {
		return false;
	}

	@Override
	public long getSize() {
		try {
			return this.smbFile.length();
		}
		catch (SmbException se) {
			logger.error("Unable to determine file size", se);
			return 0L;
		}
	}

	@Override
	public long getModified() {
		return this.smbFile.getLastModified();
	}

	@Override
	public String getFilename() {
		return this.smbFile.getName();
	}

	/**
	 * An Access Control Entry (ACE) is an element in a security descriptor
	 * such as those associated with files and directories. The Windows OS
	 * determines which users have the necessary permissions to access objects
	 * based on these entries.
	 * @return a list of Access Control Entry (ACE) objects representing
	 * the security descriptor associated with this file or directory.
	 */
	@Override
	public String getPermissions() {
		ACE[] aceArray = null;
		try {
			aceArray = this.smbFile.getSecurity(true);
		}
		catch (IOException se) {
			logger.error("Unable to determine security descriptor information for this SmbFile", se);
		}
		return (aceArray == null) ? null : Arrays.toString(aceArray);
	}

	@Override
	public SmbFile getFileInfo() {
		return this.smbFile;
	}

}
