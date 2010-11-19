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

package org.springframework.integration.file.synchronization;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.file.filters.FileListFilter;

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
public abstract class AbstractInboundRemoteFileSystemSychronizer<F> implements InitializingBean {

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
	 * @param usefulContextOrClientData
	 *            this is context information to be passed to the individual {@link EntryAcknowledgmentStrategy}.
	 *            {@link EntryAcknowledgmentStrategy#acknowledge(Object, Object)} will be called in line with the
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
	protected final void acknowledge(Object usefulContextOrClientData, F file) throws Exception {
		if (this.entryAcknowledgmentStrategy != null) {
			this.entryAcknowledgmentStrategy.acknowledge(usefulContextOrClientData, file);
		}
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	/**
	 * This is the callback where the subclasses must synchronize.
	 */
	protected abstract void syncRemoteToLocalFileSystem(Resource localDirectory);


	/**
	 * Strategy interface to expose a hook for dispatching, moving, or deleting
	 * the file once it has been delivered. This will typically be a NOOP for the
	 * implementation. Adapters should (for consistency) expose an attribute
	 * dictating whether the adapter will delete the <emphasis>source</emphasis>
	 * entry on the remote file system. This is the file-system version of an
	 * <code>ack-mode</code>. Future implementations should consider exposing a
	 * custom attribute that plugs a custom {@link EntryAcknowledgmentStrategy}
	 * into the pipeline and also some more advanced scenarios (i.e., 'move file
	 * to another folder on delete ', or 'rename on delete')
	 * 
	 * @param <F> the file entry type (file, sftp, ftp, ...)
	 */
	public static interface EntryAcknowledgmentStrategy<F> {

		/**
		 * Semantics are simple. You get a pointer to the entry just processed
		 * and any kind of helper data you could ask for. Since the strategy is
		 * a singleton and the clients you might ask for as context data are
		 * pooled, it's not recommended that you try to cache them.
		 * 
		 * @param useful
		 *            any context data
		 * @param msg
		 *            the data / file / entry you want to process -- specific to subclasses
		 * @throws Exception in case of an error while acknowledging
		 */
		void acknowledge(Object useful, F msg) throws Exception;

	}

}
