/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.Path;

import org.springframework.lang.Nullable;

/**
 * An event that is emitted when a file is written.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class FileWrittenEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private final String remoteHandle;

	private transient Path file;

	private final int dataLen;

	public FileWrittenEvent(Object source, String remoteHandle, Path file, int dataLen, @Nullable Throwable thrown) {
		super(source, thrown);
		this.remoteHandle = remoteHandle;
		this.file = file;
		this.dataLen = dataLen;
	}

	public String getRemoteHandle() {
		return this.remoteHandle;
	}

	@Nullable
	public Path getFile() {
		return this.file;
	}

	public int getDataLen() {
		return this.dataLen;
	}

	@Override
	public String toString() {
		return "FileWrittenEvent [remoteHandle=" + this.remoteHandle
				+ ", file=" + this.file
				+ ", dataLen=" + this.dataLen
				+ (this.cause == null ? "" : ", cause=" + this.cause)
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
