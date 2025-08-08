/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.remote;

import java.util.Date;

import org.springframework.integration.json.SimpleJsonSerializer;

/**
 * Abstract implementation of {@link FileInfo}; provides a setter
 * for the remote directory and a generic toString implementation.
 *
 * @param <F> The target protocol file type.
 *
 * @author Gary Russell
 *
 * @since 2.1
 */
public abstract class AbstractFileInfo<F> implements FileInfo<F>, Comparable<FileInfo<F>> {

	private String remoteDirectory;

	/**
	 * @param remoteDirectory the remoteDirectory to set
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	@Override
	public String getRemoteDirectory() {
		return this.remoteDirectory;
	}

	@Override
	public int compareTo(FileInfo<F> o) {
		return this.getFilename().compareTo(o.getFilename());
	}

	public String toJson() {
		return SimpleJsonSerializer.toJson(this, "fileInfo");
	}

	@Override
	public String toString() {
		return "FileInfo [isDirectory=" + isDirectory() + ", isLink=" + isLink()
				+ ", Size=" + getSize() + ", ModifiedTime="
				+ new Date(getModified()) + ", Filename=" + getFilename()
				+ ", RemoteDirectory=" + getRemoteDirectory() + ", Permissions=" + getPermissions() + "]";
	}

}
