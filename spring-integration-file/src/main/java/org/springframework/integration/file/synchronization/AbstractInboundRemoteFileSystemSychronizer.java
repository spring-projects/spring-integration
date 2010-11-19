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

import java.util.concurrent.ScheduledFuture;

import org.springframework.core.io.Resource;
import org.springframework.integration.MessagingException;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.scheduling.Trigger;
import org.springframework.util.Assert;

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
public abstract class AbstractInboundRemoteFileSystemSychronizer<F> extends AbstractEndpoint {

	/**
	 * Should we <emphasis>delete</emphasis> the <b>source</b> file? For an FTP
	 * server, for example, this would delete the original FTPFile instance.
	 */
	protected boolean shouldDeleteSourceFile;

	/**
	 * The directory to which we write our synchronizations.
	 */
	protected volatile Resource localDirectory;

	/**
	 * An {@link FileListFilter} that runs against the <emphasis>remote</emphasis> file system view.
	 */
	protected volatile FileListFilter<F> filter = new AcceptAllFileListFilter<F>();

	/**
	 * The {@link ScheduledFuture} instance we get when we schedule our SynchronizeTask.
	 */
	protected ScheduledFuture<?> scheduledFuture;

	/**
	 * The {@link EntryAcknowledgmentStrategy} implementation.
	 */
	protected EntryAcknowledgmentStrategy<F> entryAcknowledgmentStrategy;


	public void setEntryAcknowledgmentStrategy(EntryAcknowledgmentStrategy<F> entryAcknowledgmentStrategy) {
		this.entryAcknowledgmentStrategy = entryAcknowledgmentStrategy;
	}

	public void setShouldDeleteSourceFile(boolean shouldDeleteSourceFile) {
		this.shouldDeleteSourceFile = shouldDeleteSourceFile;
	}

	public void setLocalDirectory(Resource localDirectory) {
		this.localDirectory = localDirectory;
	}

	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
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
	 * @throws Throwable
	 *             escape hatch exception, let the adapter deal with it.
	 */
	protected void acknowledge(Object usefulContextOrClientData, F file) throws Throwable {
		Assert.notNull(this.entryAcknowledgmentStrategy != null,
				"entryAcknowledgmentStrategy can't be null!");
		this.entryAcknowledgmentStrategy.acknowledge(usefulContextOrClientData, file);
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doStart() {
		if (this.entryAcknowledgmentStrategy == null) {
			this.entryAcknowledgmentStrategy = new EntryAcknowledgmentStrategy<F>() {
				public void acknowledge(Object o, F msg) {
					// no-op
				}
			};
		}
		this.scheduledFuture = this.getTaskScheduler().schedule(new SynchronizeTask(), this.getTrigger());
	}

	/**
	 * {@inheritDoc}
	 */
	protected void doStop() {
		if (this.scheduledFuture != null) {
			this.scheduledFuture.cancel(true);
		}
	}

	/**
	 * Returns the {@link Trigger} that dictates how frequently the trigger should fire.
	 */
	protected abstract Trigger getTrigger();

	/**
	 * This is the callback where we need the implementation to do some specific  work
	 */
	protected abstract void syncRemoteToLocalFileSystem();


	/**
	 * This {@link Runnable} is launched as a background thread and is used to manage the
	 * {@link AbstractInboundRemoteFileSystemSychronizer#localDirectory} by queueing and
	 * delivering accumulated files as possible.
	 */
	class SynchronizeTask implements Runnable {
		public void run() {
			try {
				syncRemoteToLocalFileSystem();
			}
			catch (RuntimeException e) {
				throw e;
			}
			catch (Exception e) {
				throw new MessagingException("failure occurred in synchronization task", e);
			}
		}
	}


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
