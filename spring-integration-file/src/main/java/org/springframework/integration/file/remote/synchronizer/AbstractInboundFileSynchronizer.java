/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.file.remote.synchronizer;

import java.io.BufferedOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.ResettableFileListFilter;
import org.springframework.integration.file.filters.ReversibleFileListFilter;
import org.springframework.integration.file.remote.RemoteFileTemplate;
import org.springframework.integration.file.remote.session.Session;
import org.springframework.integration.file.remote.session.SessionFactory;
import org.springframework.integration.file.support.FileUtils;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.lang.Nullable;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * Base class charged with knowing how to connect to a remote file system,
 * scan it for new files and then download the files.
 * <p>
 * The implementation should run through any configured
 * {@link org.springframework.integration.file.filters.FileListFilter}s to
 * ensure the file entry is acceptable.
 *
 * @param <F> the Type that represents a remote file.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractInboundFileSynchronizer<F>
		implements InboundFileSynchronizer, BeanFactoryAware, BeanNameAware, InitializingBean, Closeable {

	protected static final ExpressionParser EXPRESSION_PARSER = new SpelExpressionParser();

	protected final Log logger = LogFactory.getLog(this.getClass()); // NOSONAR

	private final RemoteFileTemplate<F> remoteFileTemplate;

	private EvaluationContext evaluationContext;

	private String remoteFileSeparator = "/";

	/**
	 * Extension used when downloading files. We change it right after we know it's downloaded.
	 */
	private String temporaryFileSuffix = ".writing";

	private Expression localFilenameGeneratorExpression;

	/**
	 * the path on the remote mount as a String.
	 */
	private Expression remoteDirectoryExpression;

	/**
	 * An {@link FileListFilter} that runs against the <em>remote</em> file system view.
	 */
	@Nullable
	private FileListFilter<F> filter;

	/**
	 * Should we <em>delete</em> the remote <b>source</b> files
	 * after copying to the local directory? By default this is false.
	 */
	private boolean deleteRemoteFiles;

	/**
	 * Should we <em>transfer</em> the remote file <b>timestamp</b>
	 * to the local file? By default this is false.
	 */
	private boolean preserveTimestamp;

	private BeanFactory beanFactory;

	@Nullable
	private Comparator<F> comparator;

	private MetadataStore remoteFileMetadataStore = new SimpleMetadataStore();

	private String metadataStorePrefix;

	private String name;

	/**
	 * Create a synchronizer with the {@link SessionFactory} used to acquire {@link Session} instances.
	 * @param sessionFactory The session factory.
	 */
	public AbstractInboundFileSynchronizer(SessionFactory<F> sessionFactory) {
		Assert.notNull(sessionFactory, "sessionFactory must not be null");
		this.remoteFileTemplate = new RemoteFileTemplate<F>(sessionFactory);
	}

	@Nullable
	protected Comparator<F> getComparator() {
		return this.comparator;
	}

	/**
	 * Set a comparator to sort the retrieved list of {@code F} (the Type that represents
	 * the remote file) prior to applying filters and max fetch size.
	 * @param comparator the comparator.
	 * @since 5.1
	 */
	public void setComparator(@Nullable Comparator<F> comparator) {
		this.comparator = comparator;
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
	 * Set an expression used to determine the local file name.
	 * @param localFilenameGeneratorExpression the expression.
	 * @since 4.3.13
	 * @see #setRemoteDirectoryExpression(Expression)
	 */
	public void setLocalFilenameGeneratorExpressionString(String localFilenameGeneratorExpression) {
		setLocalFilenameGeneratorExpression(EXPRESSION_PARSER.parseExpression(localFilenameGeneratorExpression));
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
	 * @param remoteDirectoryExpression The remote directory expression.
	 * @since 4.2
	 */
	public void setRemoteDirectoryExpression(Expression remoteDirectoryExpression) {
		doSetRemoteDirectoryExpression(remoteDirectoryExpression);
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 * @param remoteDirectoryExpression The remote directory expression.
	 * @since 4.3.13
	 * @see #setRemoteDirectoryExpression(Expression)
	 */
	public void setRemoteDirectoryExpressionString(String remoteDirectoryExpression) {
		doSetRemoteDirectoryExpression(EXPRESSION_PARSER.parseExpression(remoteDirectoryExpression));
	}


	protected final void doSetRemoteDirectoryExpression(Expression expression) {
		Assert.notNull(expression, "'remoteDirectoryExpression' must not be null");
		this.remoteDirectoryExpression = expression;
	}

	/**
	 * Set the filter to be applied to the remote files before transferring.
	 * @param filter the file list filter.
	 */
	public void setFilter(@Nullable FileListFilter<F> filter) {
		doSetFilter(filter);
	}

	protected final void doSetFilter(@Nullable FileListFilter<F> filterToSet) {
		this.filter = filterToSet;
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

	/**
	 * Configure a {@link MetadataStore} to hold a remote file info (host, port, remote directory)
	 * to transfer downstream in message headers when local file is pulled.
	 * @param remoteFileMetadataStore the {@link MetadataStore} to use.
	 * @since 5.2
	 */
	public void setRemoteFileMetadataStore(MetadataStore remoteFileMetadataStore) {
		this.remoteFileMetadataStore = remoteFileMetadataStore;
	}

	/**
	 * Specify a prefix for keys in metadata store do not clash with other keys in the shared store.
	 * @param metadataStorePrefix the prefix to use.
	 * @since 5.2
	 * @see #setRemoteFileMetadataStore(MetadataStore)
	 */
	public void setMetadataStorePrefix(String metadataStorePrefix) {
		this.metadataStorePrefix = metadataStorePrefix;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanName(String name) {
		this.name = name;
	}

	@Override
	public final void afterPropertiesSet() {
		Assert.state(this.remoteDirectoryExpression != null, "'remoteDirectoryExpression' must not be null");
		if (this.evaluationContext == null) {
			this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
		}
		if (!StringUtils.hasText(this.metadataStorePrefix)) {
			this.metadataStorePrefix = this.name;
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
		String remoteDirectory = this.remoteDirectoryExpression.getValue(this.evaluationContext, String.class);
		if (this.logger.isTraceEnabled()) {
			this.logger.trace("Synchronizing " + remoteDirectory + " to " + localDirectory);
		}
		try {
			int transferred = this.remoteFileTemplate.execute(session ->
					transferFilesFromRemoteToLocal(remoteDirectory, localDirectory, maxFetchSize, session));
			if (this.logger.isDebugEnabled()) {
				this.logger.debug(transferred + " files transferred from '" + remoteDirectory + "'");
			}
		}
		catch (Exception e) {
			throw new MessagingException("Problem occurred while synchronizing '"
					+ remoteDirectory + "' to local directory", e);
		}
	}

	private Integer transferFilesFromRemoteToLocal(String remoteDirectory, File localDirectory,
			int maxFetchSize, Session<F> session) throws IOException {

		F[] files = session.list(remoteDirectory);
		if (!ObjectUtils.isEmpty(files)) {
			files = FileUtils.purgeUnwantedElements(files, e -> !isFile(e), this.comparator);
		}
		if (!ObjectUtils.isEmpty(files)) {
			boolean haveFilter = this.filter != null;
			boolean filteringOneByOne = haveFilter && this.filter.supportsSingleFileFiltering();
			List<F> filteredFiles = applyFilter(files, haveFilter, filteringOneByOne, maxFetchSize);

			int copied = filteredFiles.size();
			int accepted = 0;

			EvaluationContext localFileEvaluationContext = null;
			if (this.localFilenameGeneratorExpression != null) {
				localFileEvaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
				localFileEvaluationContext.setVariable("remoteDirectory", remoteDirectory);
			}

			for (F file : filteredFiles) {
				if (filteringOneByOne) {
					if ((maxFetchSize < 0 || accepted < maxFetchSize) && this.filter
							.accept(file)) { // NOSONAR never null
						accepted++;
					}
					else {
						file = null;
						copied--;
					}
				}
				copied =
						copyIfNotNull(remoteDirectory, localDirectory, localFileEvaluationContext, session,
								filteringOneByOne, filteredFiles, copied, file);
			}
			return copied;
		}
		else {
			return 0;
		}
	}

	private int copyIfNotNull(String remoteDirectory, File localDirectory,
			@Nullable EvaluationContext localFileEvaluationContext, Session<F> session, boolean filteringOneByOne,
			List<F> filteredFiles, int copied, @Nullable F file) throws IOException {

		boolean renamedFailed = false;
		try {
			if (file != null &&
					!copyFileToLocalDirectory(remoteDirectory, localFileEvaluationContext, file, localDirectory,
							session)) {

				renamedFailed = true;
			}
		}
		catch (RuntimeException | IOException e1) {
			if (filteringOneByOne) {
				resetFilterIfNecessary(file);
			}
			else {
				rollbackFromFileToListEnd(filteredFiles, file);
			}
			throw e1;
		}
		return renamedFailed ? copied - 1 : copied;
	}

	private List<F> applyFilter(F[] files, boolean haveFilter, boolean filteringOneByOne, int maxFetchSize) {
		List<F> filteredFiles;
		if (!filteringOneByOne && haveFilter) {
			filteredFiles = filterFiles(files);
		}
		else {
			filteredFiles = Arrays.asList(files);
		}
		if (maxFetchSize >= 0 && filteredFiles.size() > maxFetchSize && !filteringOneByOne) {
			if (haveFilter) {
				rollbackFromFileToListEnd(filteredFiles, filteredFiles.get(maxFetchSize));
			}
			filteredFiles = filteredFiles.stream()
					.limit(maxFetchSize)
					.collect(Collectors.toList());
		}
		return filteredFiles;
	}

	protected void rollbackFromFileToListEnd(List<F> filteredFiles, F file) {
		if (this.filter instanceof ReversibleFileListFilter) {
			((ReversibleFileListFilter<F>) this.filter)
					.rollback(file, filteredFiles);
		}
	}

	protected boolean copyFileToLocalDirectory(String remoteDirectoryPath, // NOSONAR
			@Nullable EvaluationContext localFileEvaluationContext, F remoteFile, File localDirectory,
			Session<F> session) throws IOException {

		String remoteFileName = getFilename(remoteFile);
		String localFileName = generateLocalFileName(remoteFileName, localFileEvaluationContext);
		String remoteFilePath = remoteDirectoryPath != null
				? (remoteDirectoryPath + this.remoteFileSeparator + remoteFileName)
				: remoteFileName;

		if (!isFile(remoteFile)) {
			if (this.logger.isDebugEnabled()) {
				this.logger.debug("cannot copy, not a file: " + remoteFilePath);
			}
			return false;
		}

		long modified = getModified(remoteFile);

		File localFile = new File(localDirectory, localFileName);
		boolean exists = localFile.exists();
		if (!exists || (this.preserveTimestamp && modified != localFile.lastModified())) {
			if (!exists &&
					localFileName.replaceAll("/", Matcher.quoteReplacement(File.separator)).contains(File.separator)) {
				localFile.getParentFile().mkdirs(); //NOSONAR - will fail on the writing below
			}

			boolean transfer = true;

			if (exists && !localFile.delete()) {
				transfer = false;
				if (this.logger.isInfoEnabled()) {
					this.logger.info("Cannot delete local file '" + localFile +
							"' in order to transfer modified remote file '" + remoteFile + "'. " +
							"The local file may be busy in some other process.");
				}
			}

			boolean renamed = false;

			if (transfer) {
				renamed = copyRemoteContentToLocalFile(session, remoteFilePath, localFile);
			}

			if (renamed) {
				if (this.deleteRemoteFiles) {
					session.remove(remoteFilePath);
					if (this.logger.isDebugEnabled()) {
						this.logger.debug("deleted remote file: " + remoteFilePath);
					}
				}
				if (this.preserveTimestamp && !localFile.setLastModified(modified)) {
					throw new IllegalStateException("Could not sent last modified on file: " + localFile);
				}
				String hostPort = session.getHostPort();
				int colonIndex = hostPort.lastIndexOf(':');
				String host = hostPort.substring(0, colonIndex);
				String port = hostPort.substring(colonIndex + 1);
				try {
					String remoteFileMetadata =
							new URI(protocol(), null, host, Integer.parseInt(port),
									'/' + remoteDirectoryPath, null, remoteFileName)
									.toString();
					this.remoteFileMetadataStore.put(buildMetadataKey(localFile), remoteFileMetadata);
				}
				catch (URISyntaxException ex) {
					throw new IllegalStateException("Cannot create a remote file metadata", ex);
				}
				return true;
			}
			else {
				resetFilterIfNecessary(remoteFile);
			}
		}
		else if (this.logger.isWarnEnabled()) {
			this.logger.warn("The remote file '" + remoteFile + "' has not been transferred " +
					"to the existing local file '" + localFile + "'. Consider removing the local file.");
		}

		return false;
	}

	private void resetFilterIfNecessary(F remoteFile) {
		if (this.filter instanceof ResettableFileListFilter) {
			if (this.logger.isInfoEnabled()) {
				this.logger.info("Removing the remote file '" + remoteFile +
						"' from the filter for a subsequent transfer attempt");
			}
			((ResettableFileListFilter<F>) this.filter).remove(remoteFile);
		}
	}

	private boolean copyRemoteContentToLocalFile(Session<F> session, String remoteFilePath, File localFile) {
		boolean renamed;
		String tempFileName = localFile.getAbsolutePath() + this.temporaryFileSuffix;
		File tempFile = new File(tempFileName);

		try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(tempFile))) {
			session.read(remoteFilePath, outputStream);
		}
		catch (RuntimeException e) { // NOSONAR catch and throw
			throw e;
		}
		catch (Exception e) {
			throw new MessagingException("Failure occurred while copying '" + remoteFilePath
					+ "' from the remote to the local directory", e);
		}

		renamed = tempFile.renameTo(localFile);

		if (!renamed) {
			if (localFile.delete()) {
				renamed = tempFile.renameTo(localFile);
				if (!renamed && this.logger.isInfoEnabled()) {
					this.logger.info("Cannot rename '"
							+ tempFileName
							+ "' to local file '" + localFile + "' after deleting. " +
							"The local file may be busy in some other process.");
				}
			}
			else if (this.logger.isInfoEnabled()) {
				this.logger.info("Cannot delete local file '" + localFile +
						"'. The local file may be busy in some other process.");
			}
		}
		return renamed;
	}

	private String generateLocalFileName(String remoteFileName,
			@Nullable EvaluationContext localFileEvaluationContext) {

		if (this.localFilenameGeneratorExpression != null) {
			return this.localFilenameGeneratorExpression.getValue(localFileEvaluationContext, remoteFileName,
					String.class);
		}
		return remoteFileName;
	}

	/**
	 * Obtain a metadata for remote file associated with the provided local file.
	 * @param localFile the local file to retrieve metadata for.
	 * @return the metadata for remove file in the URI style:
	 * {@code protocol://host:port/remoteDirectory#remoteFileName}
	 * @since 5.2
	 */
	@Nullable
	public String getRemoteFileMetadata(File localFile) {
		String metadataKey = buildMetadataKey(localFile);
		return this.remoteFileMetadataStore.get(metadataKey);
	}

	/**
	 * Remove a metadata for remote file associated with the provided local file.
	 * @param localFile the local file to remove metadata for.
	 * @since 5.2
	 */
	public void removeRemoteFileMetadata(File localFile) {
		String metadataKey = buildMetadataKey(localFile);
		this.remoteFileMetadataStore.remove(metadataKey);
	}

	private String buildMetadataKey(File file) {
		return this.metadataStorePrefix + file.getAbsolutePath();
	}

	protected abstract boolean isFile(F file);

	protected abstract String getFilename(F file);

	protected abstract long getModified(F file);

	/**
	 * Return the protocol this synchronizer works with.
	 * @return the protocol this synchronizer works with.
	 * @since 5.2
	 */
	protected abstract String protocol();

}
