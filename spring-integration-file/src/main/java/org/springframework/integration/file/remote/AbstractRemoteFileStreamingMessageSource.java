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
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.integration.file.FileHeaders;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A message source that produces a message with an {@link InputStream} payload
 * referencing a remote file.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public abstract class AbstractRemoteFileStreamingMessageSource<F> extends AbstractMessageSource<InputStream>
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
			String remotePath = remotePath(file);
			Session<?> session = this.remoteFileTemplate.getSession();
			try {
				return getMessageBuilderFactory().withPayload(session.readRaw(remotePath))
						.setHeader(IntegrationMessageHeaderAccessor.CLOSEABLE_RESOURCE, session)
						.setHeader(FileHeaders.REMOTE_DIRECTORY, file.getRemoteDirectory())
						.setHeader(FileHeaders.REMOTE_FILE, file.getFilename())
						.build();
			}
			catch (IOException e) {
				return new MessagingException("IOException when retrieving " + remotePath, e);
			}
		}
		return null;
	}

	protected AbstractFileInfo<F> poll() {
		if (this.toBeReceived.size() == 0) {
			listFiles();
		}
		return this.toBeReceived.poll();
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
		List<AbstractFileInfo<F>> fileInfoList = asFileInfoList(filteredFiles);
		Iterator<AbstractFileInfo<F>> iterator = fileInfoList.iterator();
		while (iterator.hasNext()) {
			AbstractFileInfo<F> next = iterator.next();
			if (next.isDirectory()) {
				iterator.remove();
			}
			else {
				next.setRemoteDirectory(remoteDirectory);
			}
		}
		if (this.comparator != null) {
			Collections.sort(fileInfoList, this.comparator);
		}
		this.toBeReceived.addAll(fileInfoList);
	}

	abstract protected List<AbstractFileInfo<F>> asFileInfoList(Collection<F> files);

}
