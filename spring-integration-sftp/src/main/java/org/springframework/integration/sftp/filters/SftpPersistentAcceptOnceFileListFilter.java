/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.sftp.filters;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.filters.AbstractPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * Persistent file list filter using the server's file timestamp to detect if we've already
 * 'seen' this file.
 *
 * @author Gary Russell
 * @author David Liu
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class SftpPersistentAcceptOnceFileListFilter
		extends AbstractPersistentAcceptOnceFileListFilter<SftpClient.DirEntry> {

	public SftpPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(SftpClient.DirEntry file) {
		return file.getAttributes().getModifyTime().toMillis();
	}

	@Override
	protected String fileName(SftpClient.DirEntry file) {
		return file.getFilename();
	}

	@Override
	protected boolean isDirectory(SftpClient.DirEntry file) {
		return file.getAttributes().isDirectory();
	}

}
