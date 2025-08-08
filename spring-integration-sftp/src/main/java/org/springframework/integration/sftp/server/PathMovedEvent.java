/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.Path;

import org.springframework.lang.Nullable;

/**
 * An event emitted when a path is moved.
 * @author Gary Russell
 *
 * @since 5.2
 *
 */
public class PathMovedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private transient Path srcPath;

	private transient Path dstPath;

	public PathMovedEvent(Object source, Path srcPath, Path dstPath, @Nullable Throwable thrown) {
		super(source, thrown);
		this.srcPath = srcPath;
		this.dstPath = dstPath;
	}

	@Nullable
	public Path getSrcPath() {
		return this.srcPath;
	}

	@Nullable
	public Path getDstPath() {
		return this.dstPath;
	}

	@Override
	public String toString() {
		return "PathMovedEvent [srcPath=" + this.srcPath
				+ ", dstPath=" + this.dstPath
				+ (this.cause == null ? "" : ", cause=" + this.cause)
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
