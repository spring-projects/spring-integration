/*
 * Copyright 2016-2017 the original author or authors.
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

package org.springframework.integration.file.dsl;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.function.Function;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.DirectoryScanner;
import org.springframework.integration.file.FileLocker;
import org.springframework.integration.file.FileReadingMessageSource;
import org.springframework.integration.file.config.FileListFilterFactoryBean;
import org.springframework.integration.file.filters.AcceptAllFileListFilter;
import org.springframework.integration.file.filters.AcceptOnceFileListFilter;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.IgnoreHiddenFileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.locking.NioFileLocker;
import org.springframework.util.Assert;

/**
 * A {@link MessageSourceSpec} for a {@link FileReadingMessageSource}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FileInboundChannelAdapterSpec
		extends MessageSourceSpec<FileInboundChannelAdapterSpec, FileReadingMessageSource>
		implements ComponentsRegistration {

	private final FileListFilterFactoryBean fileListFilterFactoryBean = new FileListFilterFactoryBean();

	private FileLocker locker;

	private ExpressionFileListFilter<File> expressionFileListFilter;

	FileInboundChannelAdapterSpec() {
		this.target = new FileReadingMessageSource();
	}

	FileInboundChannelAdapterSpec(Comparator<File> receptionOrderComparator) {
		this.target = new FileReadingMessageSource(receptionOrderComparator) {

			@Override
			protected void onInit() {
				try {
					setFilter(FileInboundChannelAdapterSpec.this.fileListFilterFactoryBean.getObject());
				}
				catch (Exception e) {
					throw new BeanCreationException("The bean for the [" + this + "] can not be instantiated.", e);
				}
				super.onInit();
			}

		};
	}

	/**
	 * Specify the input directory.
	 * @param directory the directory.
	 * @return the spec.
	 * @see FileReadingMessageSource#setDirectory(File)
	 */
	FileInboundChannelAdapterSpec directory(File directory) {
		this.target.setDirectory(directory);
		return _this();
	}

	/**
	 * Specify a custom scanner.
	 * @param scanner the scanner.
	 * @return the spec.
	 * @see FileReadingMessageSource#setScanner(DirectoryScanner)
	 */
	public FileInboundChannelAdapterSpec scanner(DirectoryScanner scanner) {
		this.target.setScanner(scanner);
		return _this();
	}

	/**
	 * Specify whether to create the source directory automatically if it does
	 * not yet exist upon initialization. By default, this value is
	 * <em>true</em>. If set to <em>false</em> and the
	 * source directory does not exist, an Exception will be thrown upon
	 * initialization.
	 * @param autoCreateDirectory the autoCreateDirectory.
	 * @return the spec.
	 * @see FileReadingMessageSource#setAutoCreateDirectory(boolean)
	 */
	public FileInboundChannelAdapterSpec autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	/**
	 * Configure the filter.
	 * @param filter the filter.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 */
	public FileInboundChannelAdapterSpec filter(FileListFilter<File> filter) {
		this.fileListFilterFactoryBean.setFilter(filter);
		return _this();
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param expression the SpEL expression for files filtering.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public FileInboundChannelAdapterSpec filterExpression(String expression) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(expression);
		return filter(this.expressionFileListFilter);
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param filterFunction the {@link Function} for files filtering.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public FileInboundChannelAdapterSpec filterFunction(Function<File, Boolean> filterFunction) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(new FunctionExpression<>(filterFunction));
		return filter(this.expressionFileListFilter);
	}

	/**
	 * Configure an {@link AcceptOnceFileListFilter} if {@code preventDuplicates == true},
	 * otherwise - {@link AcceptAllFileListFilter}.
	 * @param preventDuplicates true to configure an {@link AcceptOnceFileListFilter}.
	 * @return the spec.
	 */
	public FileInboundChannelAdapterSpec preventDuplicates(boolean preventDuplicates) {
		this.fileListFilterFactoryBean.setPreventDuplicates(preventDuplicates);
		return _this();
	}

	/**
	 /**
	 * Configure an {@link IgnoreHiddenFileListFilter} if {@code ignoreHidden == true}.
	 * @param ignoreHidden true to configure an {@link IgnoreHiddenFileListFilter}.
	 * @return the spec.
	 */
	public FileInboundChannelAdapterSpec ignoreHidden(boolean ignoreHidden) {
		this.fileListFilterFactoryBean.setIgnoreHidden(ignoreHidden);
		return _this();
	}

	/**
	 * Configure a {@link SimplePatternFileListFilter}.
	 * @param pattern The pattern.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter)
	 */
	public FileInboundChannelAdapterSpec patternFilter(String pattern) {
		this.fileListFilterFactoryBean.setFilenamePattern(pattern);
		return _this();
	}

	/**
	 * Configure a {@link RegexPatternFileListFilter}.
	 * @param regex The regex.
	 * @return the spec.
	 * @see FileReadingMessageSource#setFilter(FileListFilter)
	 * @see #filter(FileListFilter)
	 */
	public FileInboundChannelAdapterSpec regexFilter(String regex) {
		this.fileListFilterFactoryBean.setFilenameRegex(regex);
		return _this();
	}

	/**
	 * Set a {@link FileLocker} to be used to guard files against
	 * duplicate processing.
	 * @param locker the locker.
	 * @return the spec.
	 * @see FileReadingMessageSource#setLocker(FileLocker)
	 */
	public FileInboundChannelAdapterSpec locker(FileLocker locker) {
		Assert.isNull(this.locker,
				"The 'locker' (" + this.locker + ") is already configured for the FileReadingMessageSource");
		this.locker = locker;
		this.target.setLocker(locker);
		return _this();
	}

	/**
	 * Configure an {@link NioFileLocker}.
	 * @return the spec.
	 * @see #locker(FileLocker)
	 */
	public FileInboundChannelAdapterSpec nioLocker() {
		return locker(new NioFileLocker());
	}

	/**
	 * Set this flag if you want to make sure the internal queue is
	 * refreshed with the latest content of the input directory on each poll.
	 * @param scanEachPoll the scanEachPoll.
	 * @return the spec.
	 * @see FileReadingMessageSource#setScanEachPoll(boolean)
	 */
	public FileInboundChannelAdapterSpec scanEachPoll(boolean scanEachPoll) {
		this.target.setScanEachPoll(scanEachPoll);
		return _this();
	}

	/**
	 * Switch this {@link FileReadingMessageSource} to use its internal
	 * {@link java.nio.file.WatchService} directory scanner.
	 * @param useWatchService the {@code boolean} flag to enable the use
	 * of a {@link java.nio.file.WatchService}.
	 * @return the spec.
	 * @see #watchEvents
	 * @see FileReadingMessageSource#setUseWatchService(boolean)
	 */
	public FileInboundChannelAdapterSpec useWatchService(boolean useWatchService) {
		this.target.setUseWatchService(useWatchService);
		return this;
	}

	/**
	 * The {@link java.nio.file.WatchService} event types.
	 * If {@link #useWatchService} isn't {@code true}, this option is ignored.
	 * @param watchEvents the set of {@link FileReadingMessageSource.WatchEventType}.
	 * @return the spec.
	 * @see #useWatchService
	 * @see FileReadingMessageSource#setWatchEvents
	 */
	public FileInboundChannelAdapterSpec watchEvents(FileReadingMessageSource.WatchEventType... watchEvents) {
		this.target.setWatchEvents(watchEvents);
		return this;
	}

	@Override
	public Collection<Object> getComponentsToRegister() {
		if (this.expressionFileListFilter != null) {
			return Collections.singleton(this.expressionFileListFilter);
		}
		else {
			return null;
		}
	}

}
