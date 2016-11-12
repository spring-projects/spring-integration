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

package org.springframework.integration.file.dsl;

import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageSourceSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.remote.AbstractRemoteFileStreamingMessageSource;
import org.springframework.integration.file.remote.synchronizer.AbstractInboundFileSynchronizingMessageSource;
import org.springframework.messaging.Message;

/**
 * A {@link MessageSourceSpec} for an {@link AbstractInboundFileSynchronizingMessageSource}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link RemoteFileStreamingInboundChannelAdapterSpec} implementation type.
 * @param <MS> the target {@link AbstractInboundFileSynchronizingMessageSource} implementation type.
 *
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class
	RemoteFileStreamingInboundChannelAdapterSpec<F, S extends RemoteFileStreamingInboundChannelAdapterSpec<F, S, MS>,
		MS extends AbstractRemoteFileStreamingMessageSource<F>>
		extends MessageSourceSpec<S, MS> {

	private CompositeFileListFilter<F> filter;

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
		if (this.filter == null) {
			if (filter instanceof CompositeFileListFilter) {
				this.filter = (CompositeFileListFilter<F>) filter;
			}
			else {
				this.filter = new CompositeFileListFilter<F>();
				this.filter.addFilter(filter);
			}
			this.target.setFilter(this.filter);
		}
		else {
			this.filter.addFilter(filter);
		}
		return _this();
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
