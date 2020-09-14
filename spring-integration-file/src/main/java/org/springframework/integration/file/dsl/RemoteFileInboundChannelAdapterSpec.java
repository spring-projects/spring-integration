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

package org.springframework.integration.file.dsl;

import java.io.File;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizer;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.integration.metadata.MetadataStore;

/**
 * A {@link MessageSourceSpec} for an {@link AbstractInboundFileSynchronizingMessageSource}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link RemoteFileInboundChannelAdapterSpec} implementation type.
 * @param <MS> the target {@link AbstractInboundFileSynchronizingMessageSource} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class RemoteFileInboundChannelAdapterSpec<F, S extends RemoteFileInboundChannelAdapterSpec<F, S, MS>,
		MS extends AbstractInboundFileSynchronizingMessageSource<F>>
		extends MessageSourceSpec<S, MS>
		implements ComponentsRegistration {

	protected final AbstractInboundFileSynchronizer<F> synchronizer; // NOSONAR final

	private ExpressionFileListFilter<F> expressionFileListFilter;

	protected RemoteFileInboundChannelAdapterSpec(AbstractInboundFileSynchronizer<F> synchronizer) {
		this.synchronizer = synchronizer;
	}

	/**
	 * Configure whether the local directory should be created by the adapter.
	 * @param autoCreateLocalDirectory the autoCreateLocalDirectory
	 * @return the spec.
	 */
	public S autoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.target.setAutoCreateLocalDirectory(autoCreateLocalDirectory);
		return _this();
	}

	/**
	 * Configure the local directory to copy files to.
	 * @param localDirectory the localDirectory.
	 * @return the spec.
	 */
	public S localDirectory(File localDirectory) {
		this.target.setLocalDirectory(localDirectory);
		return _this();
	}

	/**
	 * A {@link FileListFilter} used to determine which files will generate messages
	 * after they have been synchronized.
	 * @param localFileListFilter the localFileListFilter.
	 * @return the spec.
	 * @see AbstractInboundFileSynchronizingMessageSource#setLocalFilter(FileListFilter)
	 */
	public S localFilter(FileListFilter<File> localFileListFilter) {
		this.target.setLocalFilter(localFileListFilter);
		return _this();
	}

	/**
	 * Configure the file name path separator used by the remote system. Defaults to '/'.
	 * @param remoteFileSeparator the remoteFileSeparator.
	 * @return the spec.
	 */
	public S remoteFileSeparator(String remoteFileSeparator) {
		this.synchronizer.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	/**
	 * Configure a SpEL expression to generate the local file name; the root object for
	 * the evaluation is the remote file name.
	 * @param localFilenameExpression the localFilenameExpression.
	 * @return the spec.
	 */
	public S localFilenameExpression(String localFilenameExpression) {
		return localFilenameExpression(PARSER.parseExpression(localFilenameExpression));
	}

	/**
	 * Configure a {@link Function} to be invoked to generate the local file name;
	 * argument passed to the {@code apply} method is the remote file name.
	 * @param localFilenameFunction the localFilenameFunction.
	 * @return the spec.
	 * @see FunctionExpression
	 */
	public S localFilename(Function<String, String> localFilenameFunction) {
		return localFilenameExpression(new FunctionExpression<>(localFilenameFunction));
	}

	/**
	 * Configure a SpEL expression to generate the local file name; the root object for
	 * the evaluation is the remote file name.
	 * @param localFilenameExpression the localFilenameExpression.
	 * @return the spec.
	 */
	public S localFilenameExpression(Expression localFilenameExpression) {
		this.synchronizer.setLocalFilenameGeneratorExpression(localFilenameExpression);
		return _this();
	}

	/**
	 * Configure a suffix to temporarily apply to the local filename; when copied the
	 * file is renamed to its final name. Default: '.writing'.
	 * @param temporaryFileSuffix the temporaryFileSuffix.
	 * @return the spec.
	 */
	public S temporaryFileSuffix(String temporaryFileSuffix) {
		this.synchronizer.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	/**
	 * Specify the full path to the remote directory.
	 * @param remoteDirectory the remoteDirectory.
	 * @return the spec.
	 * @see AbstractInboundFileSynchronizer#setRemoteDirectory(String)
	 */
	public S remoteDirectory(String remoteDirectory) {
		this.synchronizer.setRemoteDirectory(remoteDirectory);
		return _this();
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 * @param remoteDirectoryExpression The remote directory expression.
	 * @return the spec.
	 */
	public S remoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.synchronizer.setRemoteDirectoryExpression(remoteDirectoryExpression);
		return _this();
	}

	/**
	 * Configure a {@link FileListFilter} to be applied to the remote files before
	 * copying them.
	 * @param filter the filter.
	 * @return the spec.
	 */
	public S filter(FileListFilter<F> filter) {
		this.synchronizer.setFilter(filter);
		return _this();
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param expression the SpEL expression for files filtering.
	 * @return the spec.
	 * @see org.springframework.integration.file.FileReadingMessageSource#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S filterExpression(String expression) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(expression);
		return filter(this.expressionFileListFilter);
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param filterFunction the {@link Function} for files filtering.
	 * @return the spec.
	 * @see org.springframework.integration.file.FileReadingMessageSource#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S filterFunction(Function<F, Boolean> filterFunction) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(new FunctionExpression<>(filterFunction));
		return filter(this.expressionFileListFilter);
	}

	/**
	 * Configure a simple pattern filter (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @return the spec.
	 * @see #filter(FileListFilter)
	 */
	public abstract S patternFilter(String pattern);

	/**
	 * Configure a regex pattern filter (e.g. '[0-9].*.txt').
	 * @param regex the regex.
	 * @return the spec.
	 * @see #filter(FileListFilter)
	 */
	public abstract S regexFilter(String regex);

	/**
	 * Set to true to enable deletion of remote files after successful transfer.
	 * @param deleteRemoteFiles true to delete.
	 * @return the spec.
	 */
	public S deleteRemoteFiles(boolean deleteRemoteFiles) {
		this.synchronizer.setDeleteRemoteFiles(deleteRemoteFiles);
		return _this();
	}

	/**
	 * Set to true to enable the preservation of the remote file timestamp when transferring.
	 * @param preserveTimestamp true to preserve.
	 * @return the spec.
	 */
	public S preserveTimestamp(boolean preserveTimestamp) {
		this.synchronizer.setPreserveTimestamp(preserveTimestamp);
		return _this();
	}

	/**
	 * Specify the maximum number of remote files that will be fetched on each fetch
	 * attempt. A small number is recommended when multiple application instances are
	 * running, to avoid one instance from "grabbing" all the files.
	 * @param maxFetchSize the max fetch size.
	 * @return the spec.
	 * @see org.springframework.integration.support.management.MessageSourceManagement#setMaxFetchSize(int)
	 */
	public S maxFetchSize(int maxFetchSize) {
		this.target.setMaxFetchSize(maxFetchSize);
		return _this();
	}

	/**
	 * Configure a {@link MetadataStore} for remote files metadata.
	 * @param remoteFileMetadataStore the {@link MetadataStore} to use.
	 * @return the spec.
	 * @since 5.2
	 * @see AbstractInboundFileSynchronizer#setRemoteFileMetadataStore(MetadataStore)
	 */
	public S remoteFileMetadataStore(MetadataStore remoteFileMetadataStore) {
		this.synchronizer.setRemoteFileMetadataStore(remoteFileMetadataStore);
		return _this();
	}

	/**
	 * Configure a prefix for remote files metadata keys.
	 * @param metadataStorePrefix the metadata key prefix to use.
	 * @return the spec.
	 * @since 5.2
	 * @see #remoteFileMetadataStore
	 */
	public S metadataStorePrefix(String metadataStorePrefix) {
		this.synchronizer.setMetadataStorePrefix(metadataStorePrefix);
		return _this();
	}

	/**
	 * Configure a scanner to use for the file system scan after transfer.
	 * @param scanner the scanner.
	 * @return the spec.
	 * @since 5.2.4
	 */
	public S scanner(DirectoryScanner scanner) {
		this.target.setScanner(scanner);
		return _this();
	}

	/**
	 * Set a comparator to sort the retrieved list of {@code F} (the Type that represents
	 * the remote file) prior to applying filters and max fetch size.
	 * @param remoteComparator the {@link Comparator} for remote files.
	 * @return the spec.
	 * @since 5.2.9
	 */
	public S remoteComparator(Comparator<F> remoteComparator) {
		this.synchronizer.setComparator(remoteComparator);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		Map<Object, String> componentsToRegister = new LinkedHashMap<>();
		componentsToRegister.put(this.synchronizer, null);
		if (this.expressionFileListFilter != null) {
			componentsToRegister.put(this.expressionFileListFilter, null);
		}
		return componentsToRegister;
	}

}
