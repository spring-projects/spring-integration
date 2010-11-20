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

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.Session;

/**
 * Base class charged with knowing how to connect to a remote file system,
 * scan it for new files and then download the files.
 * <p/>
 * The implementation should run through any configured
 * {@link org.springframework.integration.file.filters.FileListFilter}s to
 * ensure the file entry is acceptable.
 * 
 * @author Josh Long
 */
public abstract class AbstractInboundFileSynchronizer<F> implements InboundFileSynchronizer, InitializingBean {

	protected final Log logger = LogFactory.getLog(this.getClass());


	/**
	 * Should we <emphasis>delete</emphasis> the <b>source</b> file? For an FTP
	 * server, for example, this would delete the original FTPFile instance.
	 */
	protected boolean shouldDeleteSourceFile;

	/**
	 * An {@link FileListFilter} that runs against the <emphasis>remote</emphasis> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * The {@link EntryAcknowledgmentStrategy} implementation.
	 */
	private EntryAcknowledgmentStrategy<F> entryAcknowledgmentStrategy;


	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	public void setEntryAcknowledgmentStrategy(EntryAcknowledgmentStrategy<F> entryAcknowledgmentStrategy) {
		this.entryAcknowledgmentStrategy = entryAcknowledgmentStrategy;
	}

	public void setShouldDeleteSourceFile(boolean shouldDeleteSourceFile) {
		this.shouldDeleteSourceFile = shouldDeleteSourceFile;
	}

	/**
	 * @param session
	 *            session that was used to retrieve the file. Will be passed to the {@link EntryAcknowledgmentStrategy}.
	 *            The {@link EntryAcknowledgmentStrategy#acknowledge(Object, Object)} will be called in line with the
	 *            {@link org.springframework.integration.core.MessageSource#receive()} call so this could conceivably
	 *            be a 'live' stateful client (a connection?) that is inappropriate to cache as it has per-request state.
	 * @param file
	 *            leverages strategy implementations to enable different
	 *            behavior. It's a hook to the file entry after it's been
	 *            successfully downloaded. Conceptually, you might delete the
	 *            remote one or rename it, etc.   
	 * @throws Exception
	 *             escape hatch exception, let the adapter deal with it.
	 */
	protected final void acknowledge(Session session, F file) throws Exception {
		if (this.entryAcknowledgmentStrategy != null) {
			this.entryAcknowledgmentStrategy.acknowledge(session, file);
		}
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}


	/**
	 * Strategy interface to expose a hook for dispatching, moving, or deleting
	 * the file once it has been delivered. Adapters should (for consistency)
	 * expose an attribute dictating whether the adapter will delete the
	 * <emphasis>source</emphasis> entry on the remote file system. This is the
	 * file-system version of an <code>ack-mode</code>.
	 * 
	 * @param <F> the file entry type (file, sftp, ftp, ...)
	 */
	public static interface EntryAcknowledgmentStrategy<F> {

		/**
		 * Semantics are simple. You get a pointer to the file just processed
		 * and the FTP Session that processed it.
		 * 
		 * @param session
		 *            the FTP session
		 * @param file
		 *            the file that has been processed
		 * @throws Exception in case of an error while acknowledging
		 */
		void acknowledge(Session session, F file) throws Exception;

	}

}
