/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.inbound;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.sftp.filters.SftpPersistentAcceptOnceFileListFilter;

/**
 * Handles the synchronization between a remote SFTP directory and a local mount.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SftpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<SftpClient.DirEntry> {

	/**
	 * Create a synchronizer with the {@code SessionFactory} used to acquire {@code Session} instances.
	 * @param sessionFactory The session factory.
	 */
	public SftpInboundFileSynchronizer(SessionFactory<SftpClient.DirEntry> sessionFactory) {
		super(sessionFactory);
		doSetFilter(new SftpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "sftpMessageSource"));
	}

	@Override
	protected boolean isFile(SftpClient.DirEntry file) {
		return file != null && file.getAttributes().isRegularFile();
	}

	@Override
	protected String getFilename(SftpClient.DirEntry file) {
		return file != null ? file.getFilename() : null;
	}

	@Override
	protected long getModified(SftpClient.DirEntry file) {
		return file.getAttributes().getModifyTime().toMillis();
	}

	@Override
	protected String protocol() {
		return "sftp";
	}

}
