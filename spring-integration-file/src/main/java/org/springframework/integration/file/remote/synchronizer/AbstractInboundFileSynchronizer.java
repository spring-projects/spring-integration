/*
 * Copyright 2002-2014 the original author or authors.
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
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.IntegrationEvaluationContextAware;
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
public abstract class AbstractInboundFileSynchronizer<F> implements InboundFileSynchronizer,
		InitializingBean, IntegrationEvaluationContextAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private volatile EvaluationContext evaluationContext;

	private volatile String remoteFileSeparator = "/";

	/**
	 * Extension used when downloading files. We change it right after we know it's downloaded.
	 */
	private volatile String temporaryFileSuffix =".writing";

	private volatile Expression localFilenameGeneratorExpression;

	/**
	 * the path on the remote mount as a String.
	 */
	private volatile String remoteDirectory;

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

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 *
	 * @param sessionFactory The session factory.
	 */
	public AbstractInboundFileSynchronizer(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
	}


	public void setRemoteFileSeparator(String remoteFileSeparator) {
		Assert.notNull(remoteFileSeparator, "'remoteFileSeparator' must not be null");
		this.remoteFileSeparator = remoteFileSeparator;
	}

	public void setLocalFilenameGeneratorExpression(Expression localFilenameGeneratorExpression) {
		Assert.notNull(localFilenameGeneratorExpression, "'localFilenameGeneratorExpression' must not be null");
		this.localFilenameGeneratorExpression = localFilenameGeneratorExpression;
	}

	public void setTemporaryFileSuffix(String temporaryFileSuffix) {
		this.temporaryFileSuffix = temporaryFileSuffix;
	}

	/**
	 * Specify the full path to the remote directory.
	 *
	 * @param remoteDirectory The remote directory.
	 */
	public void setRemoteDirectory(String remoteDirectory) {
		this.remoteDirectory = remoteDirectory;
	}

	public void setFilter(FileListFilter<F> filter) {
		this.filter = filter;
	}

	public void setDeleteRemoteFiles(boolean deleteRemoteFiles) {
		this.deleteRemoteFiles = deleteRemoteFiles;
	}

	public void setPreserveTimestamp(boolean preserveTimestamp) {
		this.preserveTimestamp = preserveTimestamp;
	}

	@Override
	public void setIntegrationEvaluationContext(EvaluationContext evaluationContext) {
		this.evaluationContext = evaluationContext;
	}

	@Override
	public final void afterPropertiesSet() {
		Assert.notNull(this.remoteDirectory, "remoteDirectory must not be null");
		Assert.notNull(this.evaluationContext, "evaluationContext must not be null");
	}

	protected final List<F> filterFiles(F[] files) {
		return (this.filter != null) ? this.filter.filterFiles(files) : Arrays.asList(files);
	}

	protected String getTemporaryFileSuffix() {
		return temporaryFileSuffix;
	}

	@Override
	public void synchronizeToLocalDirectory(final File localDirectory) {
		try {
			int transferred = this.remoteFileTemplate.execute(new SessionCallback<F, Integer>() {

				@Override
				public Integer doInSession(Session<F> session) throws IOException {
					F[] files = session.list(AbstractInboundFileSynchronizer.this.remoteDirectory);
					if (!ObjectUtils.isEmpty(files)) {
						List<F> filteredFiles = AbstractInboundFileSynchronizer.this.filterFiles(files);
						for (F file : filteredFiles) {
							try {
								if (file != null) {
									AbstractInboundFileSynchronizer.this.copyFileToLocalDirectory(
											AbstractInboundFileSynchronizer.this.remoteDirectory, file, localDirectory,
											session);
								}
							}
							catch (RuntimeException e) {
								if (AbstractInboundFileSynchronizer.this.filter instanceof ReversibleFileListFilter) {
									((ReversibleFileListFilter<F>) AbstractInboundFileSynchronizer.this.filter)
											.rollback(file, filteredFiles);
								}
								throw e;
							}
							catch (IOException e) {
								if (AbstractInboundFileSynchronizer.this.filter instanceof ReversibleFileListFilter) {
									((ReversibleFileListFilter<F>) AbstractInboundFileSynchronizer.this.filter)
											.rollback(file, filteredFiles);
								}
								throw e;
							}
						}
						return filteredFiles.size();
					}
					else {
						return 0;
					}
				}
			});
			if (logger.isDebugEnabled()) {
				logger.debug(transferred + " files transferred");
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
		String remoteFilePath = remoteDirectoryPath + remoteFileSeparator + remoteFileName;
		if (!this.isFile(remoteFile)) {
			if (logger.isDebugEnabled()) {
				logger.debug("cannot copy, not a file: " + remoteFilePath);
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
				if (e instanceof RuntimeException){
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
					if (logger.isDebugEnabled()) {
						logger.debug("deleted " + remoteFilePath);
					}
				}
			}
			if (this.preserveTimestamp) {
				localFile.setLastModified(getModified(remoteFile));
			}
		}
	}

	private String generateLocalFileName(String remoteFileName){
		if (this.localFilenameGeneratorExpression != null){
			return this.localFilenameGeneratorExpression.getValue(evaluationContext, remoteFileName, String.class);
		}
		return remoteFileName;
	}

	protected abstract boolean isFile(F file);

	protected abstract String getFilename(F file);

	protected abstract long getModified(F file);

}
