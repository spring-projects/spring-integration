/*
 * Copyright 2002-2019 the original author or authors.
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
