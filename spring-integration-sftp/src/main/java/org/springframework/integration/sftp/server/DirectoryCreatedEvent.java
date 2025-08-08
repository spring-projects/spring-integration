/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.sftp.server;

import java.nio.file.Path;
import java.util.Map;

import org.springframework.lang.Nullable;

/**
 * An event emitted when a directory is created.
 *
 * @author Gary Russell
 * @since 5.2
 *
 */
public class DirectoryCreatedEvent extends ApacheMinaSftpEvent {

	private static final long serialVersionUID = 1L;

	private transient Path path;

	private final Map<String, ?> attrs;

	public DirectoryCreatedEvent(Object source, Path path, Map<String, ?> attrs) {
		super(source);
		this.path = path;
		this.attrs = attrs;
	}

	@Nullable
	public Path getPath() {
		return this.path;
	}

	public Map<String, ?> getAttrs() {
		return this.attrs;
	}

	@Override
	public String toString() {
		return "DirectoryCreatedEvent [path=" + this.path
				+ ", attrs=" + this.attrs
				+ ", clientAddress=" + getSession().getClientAddress() + "]";
	}

}
