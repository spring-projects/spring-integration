/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb.inbound;

import org.codelibs.jcifs.smb.impl.SmbFile;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;

/**
 * An implementation of {@link AbstractInboundFileSynchronizer} for SMB.
 *
 * @author Markus Spann
 * @author Artem Bilan
 * @author Daniel Frey
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
			return _file.isFile();
		}
		catch (Exception _ex) {
			logger.warn("Unable to get resource status [" + _file + "].", _ex);
		}
		return false;
	}

	@Override
	protected String getFilename(SmbFile _file) {
		return _file.getName();
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
