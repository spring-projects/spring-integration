/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.file.remote;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.FileCopyUtils;

/**
 * A message source that streams a remote file directly into a {@code byte[]}.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class AbstractRemoteFileStreamingMessageSource<F> extends AbstractMessageSource<byte[]>
		implements BeanFactoryAware, InitializingBean {

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private final BlockingQueue<AbstractFileInfo<F>> toBeReceived = new LinkedBlockingQueue<AbstractFileInfo<F>>();

	private final Comparator<AbstractFileInfo<F>> comparator;

	/**
	 * the path on the remote server.
	 */
	private volatile Expression remoteDirectoryExpression;

	private volatile String remoteFileSeparator = "/";

	/**
	 * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * Should we <em>delete</em> the remote <b>source</b> files
	 * after copying to the local directory? By default this is false.
	 */
	private volatile boolean deleteRemoteFiles;

	protected AbstractRemoteFileStreamingMessageSource(RemoteFileTemplate<F> template,
			Comparator<AbstractFileInfo<F>> comparator) {
		this.remoteFileTemplate = template;
		this.comparator = comparator;
	}

	/**
	 * Specify the full path to the remote directory.
	 *
	 * @param remoteDirectory The remote directory.
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectoryExpression = new LiteralExpression(remoteDirectory);
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 *
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
		this.filter = filter;
	}

	/**
	 * Set to true to enable deletion of remote files after successful transfer.
	 * @param deleteRemoteFiles true to delete.
	 */
	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	protected boolean isDeleteRemoteFiles() {
		return this.deleteRemoteFiles;
	}

	protected RemoteFileTemplate<F> getRemoteFileTemplate() {
		return this.remoteFileTemplate;
	}

	@Override
	public final void afterPropertiesSet() {
		Assert.state(this.remoteDirectoryExpression != null, "'remoteDirectoryExpression' must not be null");
		doInit();
	}

	/**
	 * Subclasses can override to perform initialization - called from
	 * {@link InitializingBean#afterPropertiesSet()}.
	 */
	protected void doInit() {
	}

	@Override
	protected Object doReceive() {
		AbstractFileInfo<F> file = poll();
		if (file != null) {
			if (this.deleteRemoteFiles) {
				this.remoteFileTemplate.remove(remotePath(file));
			}
			AbstractIntegrationMessageBuilder<byte[]> builder = getMessageBuilderFactory().withPayload(fetchAll(file));
			addHeaders(file, builder);
			return builder.build();
		}
		return null;
	}

	protected final void addHeaders(AbstractFileInfo<F> file,
			AbstractIntegrationMessageBuilder<byte[]> builder) {
		builder
			.setHeader(FileHeaders.REMOTE_DIRECTORY, file.getRemoteDirectory())
			.setHeader(FileHeaders.REMOTE_FILE, file.getFilename());
	}

	protected AbstractFileInfo<F> poll() {
		if (this.toBeReceived.size() == 0) {
			listFiles();
		}
		return this.toBeReceived.poll();
	}

	private byte[] fetchAll(AbstractFileInfo<F> file) {
		final ByteArrayOutputStream baos = new ByteArrayOutputStream();
		if (this.remoteFileTemplate.get(remotePath(file), new InputStreamCallback() {

			@Override
			public void doWithInputStream(InputStream stream) throws IOException {
				FileCopyUtils.copy(stream, baos);
			}

		})) {
			return baos.toByteArray();
		}
		else {
			throw new MessagingException("File retrieval failed");
		}
	}

	protected String remotePath(AbstractFileInfo<F> file) {
		String remotePath = file.getRemoteDirectory().endsWith(this.remoteFileSeparator)
				? file.getRemoteDirectory() + file.getFilename()
				: file.getRemoteDirectory() + this.remoteFileSeparator + file.getFilename();
		return remotePath;
	}

	private void listFiles() {
		String remoteDirectory = this.remoteDirectoryExpression.getValue(getEvaluationContext(), String.class);
		F[] files = this.remoteFileTemplate.list(remoteDirectory);
		List<F> filteredFiles = this.filter == null ? Arrays.asList(files) : this.filter.filterFiles(files);
		List<AbstractFileInfo<F>> fileInfoList = asFileInfoList(filteredFiles, remoteDirectory);
		Iterator<AbstractFileInfo<F>> iterator = fileInfoList.iterator();
		while (iterator.hasNext()) {
			AbstractFileInfo<F> next = iterator.next();
			if (next.isDirectory()) {
				iterator.remove();
			}
		}
		if (this.comparator != null) {
			Collections.sort(fileInfoList, this.comparator);
		}
		this.toBeReceived.addAll(fileInfoList);
	}

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files, String remoteDirectory);

}
