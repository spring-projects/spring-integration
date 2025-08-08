/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.inbound;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.ftp.filters.FtpPersistentAcceptOnceFileListFilter;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * An implementation of {@link AbstractInboundFileSynchronizer} for FTP.
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 */
public class FtpInboundFileSynchronizer extends AbstractInboundFileSynchronizer<FTPFile> {

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire
	 * {@link org.springframework.integration.file.remote.session.Session} instances.
	 * @param sessionFactory The session factory.
	 */
	public FtpInboundFileSynchronizer(SessionFactory<FTPFile> sessionFactory) {
		super(sessionFactory);
		doSetRemoteDirectoryExpression(new LiteralExpression(null)); // NOSONAR - LE can actually handle null ok
		doSetFilter(new FtpPersistentAcceptOnceFileListFilter(new SimpleMetadataStore(), "ftpMessageSource"));
	}

	@Override
	protected boolean isFile(FTPFile file) {
		return file != null && file.isFile();
	}

	@Override
	protected String getFilename(FTPFile file) {
		return (file != null ? file.getName() : null);
	}

	@Override
	protected long getModified(FTPFile file) {
		return file.getTimestamp().getTimeInMillis();
	}

	@Override
	protected String protocol() {
		return "ftp";
	}

}
