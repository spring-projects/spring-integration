/**
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.smb.inbound;

import jcifs.smb.SmbFile;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;

/**
 * An implementation of {@link AbstractInboundFileSynchronizer} for SMB.
 *
 * @author Markus Spann
 * @since 2.1.1
 */
public class SmbInboundFileSynchronizer extends AbstractInboundFileSynchronizer<SmbFile> {

	private final Log    logger = LogFactory.getLog(SmbInboundFileSynchronizer.class);

	private final String toString;

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire 
	 * {@link org.springframework.integration.file.remote.session.Session} instances.
	 */
	public SmbInboundFileSynchronizer(SessionFactory<SmbFile> _sessionFactory) {
		super(_sessionFactory);
		toString = getClass().getName() + "[sessionFactory=" + _sessionFactory + "]";
	}

	@Override
	protected boolean isFile(SmbFile _file) {
		try {
			return _file != null && _file.isFile();
		} catch (Exception _ex) {
			logger.warn("Unable to get resource status [" + _file + "].", _ex);
		}
		return false;
	}

	@Override
	protected String getFilename(SmbFile _file) {
		return _file != null ? _file.getName() : null;
	}

	@Override
	public String toString() {
		return toString;
	}

}
