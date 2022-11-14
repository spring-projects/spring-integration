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
