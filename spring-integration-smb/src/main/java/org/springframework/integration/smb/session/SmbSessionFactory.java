/*
 * Copyright 2012-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.smb.session;

import java.io.IOException;
import java.util.Properties;

import jcifs.CIFSContext;
import jcifs.smb.SmbFile;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;

/**
 * The SMB session factory.
 *
 * @author Markus Spann
 * @author Gregory Bragg
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class SmbSessionFactory extends SmbConfig implements SessionFactory<SmbFile> {

	private static final Log logger = LogFactory.getLog(SmbSessionFactory.class);

	private CIFSContext context = null;

	public SmbSessionFactory() {
		logger.debug("New " + getClass().getName() + " created.");
	}

	/**
	 * Initializes the SMB session factory with a custom client context configuration.
	 * @param _context that holds the client configuration, shared services as well as the active credentials
	 */
	public SmbSessionFactory(CIFSContext _context) {
		Assert.notNull(_context, "_context can't be null");
		this.context = _context;
		logger.debug("New " + getClass().getName() + " created with CIFSContext.");
	}

	@Override
	public final SmbSession getSession() {
		try {
			return createSession();
		}
		catch (Exception _ex) {
			throw new IllegalStateException("Failed to create session.", _ex);
		}
	}

	protected SmbSession createSession() throws IOException {
		SmbShare smbShare;
		if (this.context != null) {
			smbShare = new SmbShare(this, this.context);
		}
		else {
			Properties props = new Properties();
			props.setProperty("jcifs.smb.client.minVersion", this.getSmbMinVersion().name());
			props.setProperty("jcifs.smb.client.maxVersion", this.getSmbMaxVersion().name());

			smbShare = new SmbShare(this, props);
		}

		if (logger.isInfoEnabled()) {
			logger.info(String.format("SMB share init: %s/%s", getHostPort(), getShareAndDir()));
		}

		smbShare.init();
		logger.debug("SMB share initialized.");

		return new SmbSession(smbShare);
	}

}
