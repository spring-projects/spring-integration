/*
 * Copyright © 2012 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb.inbound;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;

/**
 * An implementation of {@link AbstractInboundFileSynchronizer} for SMB.
 *
 * @author Markus Spann
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbInboundFileSynchronizer extends AbstractInboundFileSynchronizer<SmbFile> {

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire
	 * {@link org.springframework.integration.file.remote.session.Session} instances.
	 * @param sessionFactory the {@link SessionFactory} to use.
	 */
	public SmbInboundFileSynchronizer(SessionFactory<SmbFile> sessionFactory) {
		super(sessionFactory);
	}

	@Override
	protected boolean isFile(SmbFile _file) {
		try {
			return _file != null && _file.isFile();
		}
		catch (Exception _ex) {
			logger.warn("Unable to get resource status [" + _file + "].", _ex);
		}
		return false;
	}

	@Override
	protected String getFilename(SmbFile _file) {
		return _file != null ? _file.getName() : null;
	}

	@Override
	protected long getModified(SmbFile file) {
		return file.getLastModified();
	}

	@Override
	protected String protocol() {
		return "smb";
	}

}
