/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.file.remote;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.endpoint.AbstractFetchLimitingMessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.support.FileUtils;
import org.springframework.integration.support.management.ManageableLifecycle;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * A message source that produces a message with an {@link InputStream} payload
 * referencing a remote file.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Lukas Gemela
 *
 * @since 4.3
 *
 */
public abstract class AbstractRemoteFileStreamingMessageSource<F>
		extends AbstractFetchLimitingMessageSource<InputStream> implements ManageableLifecycle {

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private final BlockingQueue<AbstractFileInfo<F>> toBeReceived = new LinkedBlockingQueue<>();

	private final Comparator<F> comparator;

	private final AtomicBoolean running = new AtomicBoolean();

	private final AtomicInteger fetched = new AtomicInteger();

	private boolean fileInfoJson = true;

	/**
	 * the path on the remote server.
	 */
	private Expression remoteDirectoryExpression;

	private String remoteFileSeparator = "/";

	/**
	 * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private FileListFilter<F> filter;

	protected AbstractRemoteFileStreamingMessageSource(RemoteFileTemplate<F> template,
			@Nullable Comparator<F> comparator) {

		Assert.notNull(template, "'template' must not be null");
		this.remoteFileTemplate = template;
		this.comparator = comparator;
	}

	/**
	 * Specify the full path to the remote directory.
	 * @param remoteDirectory The remote directory.
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectoryExpression = new LiteralExpression(remoteDirectory);
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 * @param remoteDirectoryExpression The remote directory expression.
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "'remoteDirectoryExpression' must not be null");
		this.remoteDirectoryExpression = remoteDirectoryExpression;
	}

	/**
	 * Set the remote file separator; default '/'
	 * @param remoteFileSeparator the remote file separator.
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	/**
	 * Set the filter to be applied to the remote files before transferring.
	 * @param filter the file list filter.
	 */
	public void setFilter(FileListFilter<F> filter) {
		doSetFilter(filter);
	}

	protected final void doSetFilter(FileListFilter<F> filterToSet) {
		this.filter = filterToSet;
	}

	/**
	 * Set to false to add the {@link FileHeaders#REMOTE_FILE_INFO} header to the raw {@link FileInfo}.
	 * Default is true meaning that common file information properties are provided
	 * in that header as JSON.
	 * @param fileInfoJson false to set the raw object.
	 * @since 5.0
	 */
	public void setFileInfoJson(boolean fileInfoJson) {
		this.fileInfoJson = fileInfoJson;
	}

	protected RemoteFileTemplate<F> getRemoteFileTemplate() {
		return this.remoteFileTemplate;
	}

	@Override
	public final void onInit() {
		Assert.state(this.remoteDirectoryExpression != null, "'remoteDirectoryExpression' must not be null");
		doInit();
	}

	/**
	 * Subclasses can override to perform initialization - called from
	 * {@link org.springframework.beans.factory.InitializingBean#afterPropertiesSet()}.
	 */
	protected void doInit() {
	}


	@Override
	public void start() {
		this.running.set(true);
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false)) {
			if (this.filter == null || this.filter.supportsSingleFileFiltering()) {
				this.toBeReceived.clear();
			}
			else {
				// remove unprocessed files from the queue (and filter)
				AbstractFileInfo<F> file = this.toBeReceived.poll();
				while (file != null) {
					resetFilterIfNecessary(file);
					file = this.toBeReceived.poll();
				}
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

	@Override
	protected Object doReceive(int maxFetchSize) {
		Assert.state(this.running.get(), () -> getComponentName() + " is not running");
		if (maxFetchSize > 0 && this.fetched.get() >= maxFetchSize) {
			this.toBeReceived.clear();
			this.fetched.set(0);
		}
		AbstractFileInfo<F> file = poll();
		while (file != null) {
			if (this.filter != null && this.filter.supportsSingleFileFiltering()
					&& !this.filter.accept(file.getFileInfo())) {

				if (this.toBeReceived.size() > 0) { // don't re-fetch already filtered files
					file = poll();
					continue;
				}
				else {
					break;
				}
			}
			if (maxFetchSize > 0) {
				this.fetched.incrementAndGet();
			}
			return remoteFileToMessage(file);
		}
		return null;
	}

	private Object remoteFileToMessage(AbstractFileInfo<F> file) {
		try {
			String remotePath = remotePath(file);
			Session<?> session = this.remoteFileTemplate.getSession();
			try {
				return getMessageBuilderFactory()
						.withPayload(session.readRaw(remotePath))
						.setHeader(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, session)
						.setHeader(FileHeaders.REMOTE_DIRECTORY, file.getRemoteDirectory())
						.setHeader(FileHeaders.REMOTE_FILE, file.getFilename())
						.setHeader(FileHeaders.REMOTE_HOST_PORT, session.getHostPort())
						.setHeader(FileHeaders.REMOTE_FILE_INFO,
								this.fileInfoJson ? file.toJson() : file);
			}
			catch (IOException e) {
				session.close();
				throw new UncheckedIOException("IOException when retrieving " + remotePath, e);
			}
		}
		catch (RuntimeException e) {
			resetFilterIfNecessary(file);
			throw e;
		}
	}

	protected AbstractFileInfo<F> poll() {
		if (this.toBeReceived.size() == 0) {
			listFiles();
		}
		return this.toBeReceived.poll();
	}

	private void resetFilterIfNecessary(AbstractFileInfo<F> file) {
		if (this.filter instanceof ResettableFileListFilter) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Removing the remote file '" + file +
						"' from the filter for a subsequent transfer attempt");
			}
			((ResettableFileListFilter<F>) this.filter).remove(file.getFileInfo());
		}
	}

	protected String remotePath(AbstractFileInfo<F> file) {
		return file.getRemoteDirectory().endsWith(this.remoteFileSeparator)
				? file.getRemoteDirectory() + file.getFilename()
				: file.getRemoteDirectory() + this.remoteFileSeparator + file.getFilename();
	}

	private void listFiles() {
		String remoteDirectory = this.remoteDirectoryExpression.getValue(getEvaluationContext(), String.class);
		F[] files = this.remoteFileTemplate.list(remoteDirectory);
		if (!ObjectUtils.isEmpty(files)) {
			files = FileUtils.purgeUnwantedElements(files, f -> f == null || isDirectory(f), this.comparator);
		}
		if (!ObjectUtils.isEmpty(files)) {
			List<AbstractFileInfo<F>> fileInfoList;
			if (this.filter != null && !this.filter.supportsSingleFileFiltering()) {
				int maxFetchSize = getMaxFetchSize();
				List<F> filteredFiles = this.filter.filterFiles(files);
				if (maxFetchSize > 0 && filteredFiles.size() > maxFetchSize) {
					rollbackFromFileToListEnd(filteredFiles, filteredFiles.get(maxFetchSize));
					List<F> newList = new ArrayList<>(maxFetchSize);
					for (int i = 0; i < maxFetchSize; i++) {
						newList.add(filteredFiles.get(i));
					}
					filteredFiles = newList;
				}
				fileInfoList = asFileInfoList(filteredFiles);
			}
			else {
				fileInfoList = asFileInfoList(Arrays.asList(files));
			}
			fileInfoList.forEach(fi -> fi.setRemoteDirectory(remoteDirectory));
			this.toBeReceived.addAll(fileInfoList);
		}
	}

	protected void rollbackFromFileToListEnd(List<F> filteredFiles, F file) {
		if (this.filter instanceof ReversibleFileListFilter) {
			((ReversibleFileListFilter<F>) this.filter)
					.rollback(file, filteredFiles);
		}
	}

	protected abstract List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

	protected abstract boolean isDirectory(F file);

}
