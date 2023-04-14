/*
 * Copyright 2016-2023 the original author or authors.
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

import java.util.Collections;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSourceSpec} for an {@link AbstractRemoteFileStreamingMessageSource}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link RemoteFileStreamingInboundChannelAdapterSpec} implementation type.
 * @param <MS> the target {@link AbstractRemoteFileStreamingMessageSource} implementation type.
 *
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class RemoteFileStreamingInboundChannelAdapterSpec<F,
		S extends RemoteFileStreamingInboundChannelAdapterSpec<F, S, MS>,
		MS extends AbstractRemoteFileStreamingMessageSource<F>>
		extends MessageSourceSpec<S, MS>
		implements ComponentsRegistration {

	@Nullable
	private ExpressionFileListFilter<F> expressionFileListFilter;

	/**
	 * Configure the file name path separator used by the remote system. Defaults to '/'.
	 * @param remoteFileSeparator the remoteFileSeparator.
	 * @return the spec.
	 */
	public S remoteFileSeparator(String remoteFileSeparator) {
		this.target.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	/**
	 * Specify the full path to the remote directory.
	 * @param remoteDirectory the remoteDirectory.
	 * @return the spec.
	 * @see AbstractRemoteFileStreamingMessageSource#setRemoteDirectory(String)
	 */
	public S remoteDirectory(String remoteDirectory) {
		this.target.setRemoteDirectory(remoteDirectory);
		return _this();
	}

	/**
	 * Specify an expression that evaluates to the full path to the remote directory.
	 * @param remoteDirectoryExpression The remote directory expression.
	 * @return the spec.
	 */
	public S remoteDirectory(Expression remoteDirectoryExpression) {
		this.target.setRemoteDirectoryExpression(remoteDirectoryExpression);
		return _this();
	}

	/**
	 * Specify a function that is invoked to determine the full path to the remote directory.
	 * @param remoteDirectoryFunction The remote directory function.
	 * @return the spec.
	 */
	public S remoteDirectory(Function<Message<?>, String> remoteDirectoryFunction) {
		this.target.setRemoteDirectoryExpression(new FunctionExpression<>(remoteDirectoryFunction));
		return _this();
	}

	/**
	 * Configure a {@link FileListFilter} to be applied to the remote files before
	 * copying them.
	 * @param filter the filter.
	 * @return the spec.
	 */
	public S filter(FileListFilter<F> filter) {
		this.target.setFilter(filter);
		return _this();
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param expression the SpEL expression for files filtering.
	 * @return the spec.
	 * @see AbstractRemoteFileStreamingMessageSource#setFilter(FileListFilter)
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
	 * @see AbstractRemoteFileStreamingMessageSource#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S filterFunction(Function<F, Boolean> filterFunction) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(new FunctionExpression<>(filterFunction));
		return filter(this.expressionFileListFilter);
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

	@Override
	public Map<Object, String> getComponentsToRegister() {
		if (this.expressionFileListFilter != null) {
			return Collections.singletonMap(this.expressionFileListFilter, null);
		}
		else {
			return Collections.emptyMap();
		}
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

}
