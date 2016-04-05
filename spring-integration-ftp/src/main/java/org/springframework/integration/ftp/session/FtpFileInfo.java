/*
 * Copyright 2002-2016 the original author or authors.
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

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.remote.AbstractFileInfo;
import org.springframework.integration.file.remote.FileInfo;
import org.springframework.util.Assert;

/**
 * A {@link FileInfo} implementation for FTP/FTPS.
 *
 * @author Gary Russell
 * @since 2.1
 */
public class FtpFileInfo extends AbstractFileInfo<FTPFile> {

	private final FTPFile ftpFile;

	public FtpFileInfo(FTPFile ftpFile) {
		Assert.notNull(ftpFile, "FTPFile must not be null");
		this.ftpFile = ftpFile;
	}

	@Override
	public boolean isDirectory() {
		return this.ftpFile.isDirectory();
	}

	@Override
	public boolean isLink() {
		return this.ftpFile.isSymbolicLink();
	}

	@Override
	public long getSize() {
		return this.ftpFile.getSize();
	}

	@Override
	public long getModified() {
		return this.ftpFile.getTimestamp().getTimeInMillis();
	}

	@Override
	public String getFilename() {
		return this.ftpFile.getName();
	}

	@Override
	public String getPermissions() {
		StringBuilder sb = new StringBuilder();
		if (this.ftpFile.isDirectory()) {
			sb.append("d");
		}
		else if (this.ftpFile.isSymbolicLink()) {
			sb.append("l");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.READ_PERMISSION)) {
			sb.append("r");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.WRITE_PERMISSION)) {
			sb.append("w");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.USER_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
			sb.append("x");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.READ_PERMISSION)) {
			sb.append("r");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.WRITE_PERMISSION)) {
			sb.append("w");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.GROUP_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
			sb.append("x");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.READ_PERMISSION)) {
			sb.append("r");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.WRITE_PERMISSION)) {
			sb.append("w");
		}
		else {
			sb.append("-");
		}
		if (this.ftpFile.hasPermission(FTPFile.WORLD_ACCESS, FTPFile.EXECUTE_PERMISSION)) {
			sb.append("x");
		}
		else {
			sb.append("-");
		}
		return sb.toString();
	}

	@Override
	public FTPFile getFileInfo() {
		return this.ftpFile;
	}
}
