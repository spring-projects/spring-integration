/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

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

	public Path getSrcPath() {
		return this.srcPath;
	}

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
