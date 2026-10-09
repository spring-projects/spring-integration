/*
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.Path;

import org.jspecify.annotations.Nullable;

/**
 * An event emitted when a file or directory is removed.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class PathRemovedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private transient Path path;

	private final boolean isDirectory;

	public PathRemovedEvent(Object source, Path path, boolean isDirectory, @Nullable Throwable thrown) {
		super(source, thrown);
		this.path = path;
		this.isDirectory = isDirectory;
	}

	public Path getPath() {
		return this.path;
	}

	public boolean isDirectory() {
		return this.isDirectory;
	}

	@Override
	public String toString() {
		return "PathRemovedEvent [path=" + this.path
				+ ", isDirectory=" + this.isDirectory
				+ (this.cause == null ? "" : ", cause=" + this.cause)
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
