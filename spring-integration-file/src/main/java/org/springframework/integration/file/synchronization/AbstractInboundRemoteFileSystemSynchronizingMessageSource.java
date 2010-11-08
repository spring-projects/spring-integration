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

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.regex.Pattern;

import org.springframework.core.io.Resource;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.entries.FileEntryNameExtractor;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.PatternMatchingFileListFilter;

/**
 * Factors out the common logic between the FTP and SFTP adapters. Designed to
 * be extensible to handle adapters whose task it is to synchronize a remote
 * file system with a local file system (NB: this does *NOT* handle pushing
 * files TO the remote file system that exist uniquely in the local file system.
 * It only handles pulling from the remote file system - as you would expect
 * from an 'inbound' adapter).
 * <p/>
 * The base class supports configuration of whether the remote file system and
 * local file system's directories should be created on start (what 'creating a
 * directory' means to the specific adapter is of course implementation
 * specific).
 * <p/>
 * This class is to be used as a pair with an implementation of
 * {@link AbstractInboundRemoteFileSystemSychronizer}. The synchronizer must
 * handle the work of actually connecting to the remote file system and
 * delivering new {@link File}s.
 * 
 * @author Josh Long
 */
public abstract class AbstractInboundRemoteFileSystemSynchronizingMessageSource<F, S extends AbstractInboundRemoteFileSystemSychronizer<F>>
		extends MessageProducerSupport implements MessageSource<File> {

	/**
	 * Extension used when downloading files. We change it right after we know it's downloaded.
	 */
	public static final String INCOMPLETE_EXTENSION = ".INCOMPLETE";

	/**
	 * Should the endpoint attempt to create the local directory and/or the remote directory?
	 */
	protected volatile boolean autoCreateDirectories = true;

	/**
	 * An implementation that will handle the chores of actually connecting to and synching up
	 * the remote file system with the local one, in an inbound direction.
	 */
	protected volatile S synchronizer;

	/**
	 * Directory to which things should be synched locally.
	 */
	protected volatile Resource localDirectory;

	/**
	 * The actual {@link FileReadingMessageSource} that monitors the local filesystem once files are synched.
	 */
	protected volatile FileReadingMessageSource fileSource;

	/**
	 * The predicate to use in scanning the remote File system for downloads.
	 */
	protected FileListFilter<F> remotePredicate;


	public void setAutoCreateDirectories(boolean autoCreateDirectories) {
		this.autoCreateDirectories = autoCreateDirectories;
	}

	public void setSynchronizer(S synchronizer) {
		this.synchronizer = synchronizer;
	}

	public void setLocalDirectory(Resource localDirectory) {
		this.localDirectory = localDirectory;
	}

	public void setRemotePredicate(FileListFilter<F> remotePredicate) {
		this.remotePredicate = remotePredicate;
	}

	@Override
	protected void onInit() {
		try {
			if (this.remotePredicate != null) {
				this.synchronizer.setFilter(this.remotePredicate);
			}
			if (this.localDirectory != null && !this.localDirectory.exists()) {
				if (this.autoCreateDirectories) {
					if (logger.isDebugEnabled()) {
						logger.debug("The '" + this.localDirectory + "' directory doesn't exist; Will create.");
					}
					this.localDirectory.getFile().mkdirs();
				}
				else {
					throw new FileNotFoundException(this.localDirectory.getFilename());
				}
			}

			/**
			 * Make sure the remote files get here.
			 */
			this.synchronizer.setLocalDirectory(this.localDirectory);
			this.synchronizer.setTaskScheduler(this.getTaskScheduler());
			this.synchronizer.setBeanFactory(this.getBeanFactory());
			this.synchronizer.setPhase(this.getPhase());
			this.synchronizer.setBeanName(this.getComponentName());

			/**
			 * Forwards files once they ultimately appear in the {@link #localDirectory}.
			 */
			this.fileSource = new FileReadingMessageSource();
			this.fileSource.setFilter(this.buildFilter());
			this.fileSource.setDirectory(this.localDirectory.getFile());
			this.fileSource.afterPropertiesSet();
			this.synchronizer.afterPropertiesSet();
		}
		catch (RuntimeException e) {
			throw e;
		}
		catch (Exception e) {
			throw new MessagingException("Failure during initialization of MessageSource for: "
					+ this.getComponentType(), e);
		}
	}

	public Message<File> receive() {
		return this.fileSource.receive();
	}

	@SuppressWarnings("unchecked")
	private FileListFilter<File> buildFilter() {
		FileEntryNameExtractor fileEntryNameExtractor = new FileEntryNameExtractor();
		Pattern completePattern = Pattern.compile("^.*(?<!" + INCOMPLETE_EXTENSION + ")$");
		return new CompositeFileListFilter<File>(Arrays.asList(
				new AcceptOnceFileListFilter<File>(),
				new PatternMatchingFileListFilter<File>(fileEntryNameExtractor, completePattern)));
	}

}
