/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.file.remote.synchronizer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.MessagingException;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class charged with knowing how to connect to a remote file system,
 * scan it for new files and then download the files.
 * <p/>
 * The implementation should run through any configured
 * {@link org.springframework.integration.file.filters.FileListFilter}s to
 * ensure the file entry is acceptable.
 * 
 * @author Josh Long
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class AbstractInboundFileSynchronizer<F> implements InboundFileSynchronizer, InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass());

	/**
	 * the path on the remote mount
	 */
	private volatile String remotePath;

	/**
	 * the {@link SessionFactory} for acquiring remote file Sessions.
	 */
	private final SessionFactory sessionFactory;

	/**
	 * An {@link FileListFilter} that runs against the <emphasis>remote</emphasis> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * Should we <emphasis>delete</emphasis> the <b>source</b> file? For an FTP
	 * server, for example, this would delete the original FTPFile instance.
	 */
	protected boolean shouldDeleteSourceFile;


	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 */
	public AbstractInboundFileSynchronizer(SessionFactory sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.sessionFactory = sessionFactory;
	}


	public void setRemotePath(String remotePath) {
		this.remotePath = remotePath;
	}

	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	public void setShouldDeleteSourceFile(boolean shouldDeleteSourceFile) {
		this.shouldDeleteSourceFile = shouldDeleteSourceFile;
	}

	public final void afterPropertiesSet() {
		Assert.notNull(this.remotePath, "remotePath must not be null");
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	public void synchronizeToLocalDirectory(File localDirectory) {
		Session session = null;
		try {
			session = this.sessionFactory.getSession();
			Assert.state(session != null, "failed to acquire a Session");
			this.synchronizeToLocalDirectory(this.remotePath, localDirectory, session);
		}
		catch (IOException e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
		finally {
			if (session != null) {
				try {
					session.close();
				}
				catch (Exception ignored) {
					if (logger.isDebugEnabled()) {
						logger.debug("failed to close Session", ignored);
					}
				}
			}
		}
	}

	private void synchronizeToLocalDirectory(String remoteDirectoryPath, File localDirectory, Session session) throws IOException {
		F[] files = session.ls(remoteDirectoryPath);
		if (!ObjectUtils.isEmpty(files)) {
			Collection<F> filteredFiles = this.filterFiles(files);
			for (F file : filteredFiles) {
				if (file != null) {
					this.copyFileToLocalDirectory(remoteDirectoryPath, file, localDirectory, session);
				}
			}
		}
	}

	protected abstract boolean copyFileToLocalDirectory(String remoteDirectoryPath, F file, File localDirectory, Session session) throws IOException;

}
