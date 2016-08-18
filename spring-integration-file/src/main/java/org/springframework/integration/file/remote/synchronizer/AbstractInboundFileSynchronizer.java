/*
 * Copyright 2002-2016 the original author or authors.
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

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.SessionCallback;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * Base class charged with knowing how to connect to a remote file system,
 * scan it for new files and then download the files.
 * <p>
 * The implementation should run through any configured
 * {@link org.springframework.integration.file.filters.FileListFilter}s to
 * ensure the file entry is acceptable.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public abstract class AbstractInboundFileSynchronizer<F>
		implements InboundFileSynchronizer, BeanFactoryAware, InitializingBean, Closeable {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private volatile EvaluationContext evaluationContext;

	private volatile String remoteFileSeparator = "/";

	/**
	 * Extension used when downloading files. We change it right after we know it's downloaded.
	 */
	private volatile String temporaryFileSuffix = ".writing";

	private volatile Expression localFilenameGeneratorExpression;

	/**
	 * the path on the remote mount as a String.
	 */
	private volatile Expression remoteDirectoryExpression;

	/**
	 * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	private volatile FileListFilter<F> filter;

	/**
	 * Should we <em>delete</em> the remote <b>source</b> files
	 * after copying to the local directory? By default this is false.
	 */
	private volatile boolean deleteRemoteFiles;

	/**
	 * Should we <em>transfer</em> the remote file <b>timestamp</b>
	 * to the local file? By default this is false.
	 */
	private volatile boolean  preserveTimestamp;

	private BeanFactory beanFactory;

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 *
	 * @param sessionFactory The session factory.
	 */
	public AbstractInboundFileSynchronizer(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
	}


	/**
	 * @param remoteFileSeparator the remote file separator.
	 * @see RemoteFileTemplate#setRemoteFileSeparator(String)
	 */
	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	/**
	 * Set an expression used to determine the local file name.
	 * @param localFilenameGeneratorExpression the expression.
	 */
	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}

	/**
	 * Set a temporary file suffix to be used while transferring files. Default ".writing".
	 * @param temporaryFileSuffix the file suffix.
	 */
	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
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
	 * @since 4.2
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		Assert.notNull(remoteDirectoryExpression, "'remoteDirectoryExpression' must not be null");
		this.remoteDirectoryExpression = remoteDirectoryExpression;
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

	/**
	 * Set to true to enable the preservation of the remote file timestamp when
	 * transferring.
	 * @param preserveTimestamp true to preserve.
	 */
	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public final void afterPropertiesSet() {
		Assert.state(this.remoteDirectoryExpression != null, "'remoteDirectoryExpression' must not be null");
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
		doInit();
	}

	/**
	 * Subclasses can override to perform initialization - called from
	 * {@link InitializingBean#afterPropertiesSet()}.
	 */
	protected void doInit() {
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	protected String getTemporaryFileSuffix() {
		return this.temporaryFileSuffix;
	}

	@Override
	public void close() throws IOException {
		if (this.filter instanceof Closeable) {
			((Closeable) this.filter).close();
		}
	}

	@Override
	public void synchronizeToLocalDirectory(final File localDirectory) {
		synchronizeToLocalDirectory(localDirectory, Integer.MIN_VALUE);
	}

	@Override
	public void synchronizeToLocalDirectory(final File localDirectory, final int maxFetchSize) {
		if (maxFetchSize == 0) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("Max Fetch Size is zero - fetch to " + localDirectory.getAbsolutePath() + " ignored");
			}
			return;
		}
		final String remoteDirectory = this.remoteDirectoryExpression.getValue(this.evaluationContext, String.class);
		try {
			int transferred = this.remoteFileTemplate.execute(new SessionCallback<F, Integer>() {

				@Override
				public Integer doInSession(Session<F> session) throws IOException {
					F[] files = session.list(remoteDirectory);
					if (!ObjectUtils.isEmpty(files)) {
						List<F> filteredFiles = filterFiles(files);
						if (maxFetchSize >= 0 && filteredFiles.size() > maxFetchSize) {
							rollbackFromFileToListEnd(filteredFiles, filteredFiles.get(maxFetchSize));
							List<F> newList = new ArrayList<>(maxFetchSize);
							for (int i = 0; i < maxFetchSize; i++) {
								newList.add(filteredFiles.get(i));
							}
							filteredFiles = newList;
						}
						for (F file : filteredFiles) {
							try {
								if (file != null) {
									copyFileToLocalDirectory(
											remoteDirectory, file, localDirectory,
											session);
								}
							}
							catch (RuntimeException e) {
								rollbackFromFileToListEnd(filteredFiles, file);
								throw e;
							}
							catch (IOException e) {
								rollbackFromFileToListEnd(filteredFiles, file);
								throw e;
							}
						}
						return filteredFiles.size();
					}
					else {
						return 0;
					}
				}

				public void rollbackFromFileToListEnd(List<F> filteredFiles, F file) {
					if (AbstractInboundFileSynchronizer.this.filter instanceof ReversibleFileListFilter) {
						((ReversibleFileListFilter<F>) AbstractInboundFileSynchronizer.this.filter)
								.rollback(file, filteredFiles);
					}
				}

			});
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(transferred + " files transferred");
			}
		}
		catch (Exception e) {
			throw new MessagingException("Problem occurred while synchronizing remote to local directory", e);
		}
	}

	protected void copyFileToLocalDirectory(String remoteDirectoryPath, F remoteFile, File localDirectory,
			Session<F> session) throws IOException {
		String remoteFileName = this.getFilename(remoteFile);
		String localFileName = this.generateLocalFileName(remoteFileName);
		String remoteFilePath = remoteDirectoryPath != null
				? (remoteDirectoryPath + this.remoteFileSeparator + remoteFileName)
				: remoteFileName;
		if (!this.isFile(remoteFile)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("cannot copy, not a file: " + remoteFilePath);
			}
			return;
		}

		File localFile = new File(localDirectory, localFileName);
		if (!localFile.exists()) {
			String tempFileName = localFile.getAbsolutePath() + this.temporaryFileSuffix;
			File tempFile = new File(tempFileName);
			OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile));
			try {
				session.read(remoteFilePath, outputStream);
			}
			catch (Exception e) {
				if (e instanceof RuntimeException) {
					throw (RuntimeException) e;
				}
				else {
					throw new MessagingException("Failure occurred while copying from remote to local directory", e);
				}
			}
			finally {
				try {
					outputStream.close();
				}
				catch (Exception ignored2) {
				}
			}

			if (tempFile.renameTo(localFile)) {
				if (this.deleteRemoteFiles) {
					session.remove(remoteFilePath);
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("deleted " + remoteFilePath);
					}
				}
			}
			if (this.preserveTimestamp) {
				localFile.setLastModified(getModified(remoteFile));
			}
		}
	}

	private String generateLocalFileName(String remoteFileName) {
		if (this.localFilenameGeneratorExpression != null) {
			return this.localFilenameGeneratorExpression.getValue(this.evaluationContext, remoteFileName, String.class);
		}
		return remoteFileName;
	}

	protected abstract boolean isFile(F file);

	protected abstract String getFilename(F file);

	protected abstract long getModified(F file);

}
