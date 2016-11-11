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

import java.io.File;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.messaging.Message;

/**
 * The {@link MessageHandlerSpec} for the {@link AbstractRemoteFileOutboundGateway}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link RemoteFileOutboundGatewaySpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class RemoteFileOutboundGatewaySpec<F, S extends RemoteFileOutboundGatewaySpec<F, S>>
		extends MessageHandlerSpec<S, AbstractRemoteFileOutboundGateway<F>> {

	private CompositeFileListFilter<F> filter;

	private CompositeFileListFilter<File> mputFilter;

	protected RemoteFileOutboundGatewaySpec(AbstractRemoteFileOutboundGateway<F> outboundGateway) {
		this.target = outboundGateway;
		this.target.setRequiresReply(true);
	}

	/**
	 * Specify the array of options for various gateway commands.
	 * @param options the options to set.
	 * @return the spec
	 * @see  #options(AbstractRemoteFileOutboundGateway.Option...)
	 */
	public S options(String options) {
		this.target.setOptions(options);
		return _this();
	}

	/**
	 * Specify the array of {@link AbstractRemoteFileOutboundGateway.Option}
	 * for various gateway commands.
	 * @param options the options to set.
	 * @return the spec
	 */
	public S options(AbstractRemoteFileOutboundGateway.Option... options) {
		this.target.setOption(options);
		return _this();
	}

	/**
	 * Set the file separator when dealing with remote files; default '/'.
	 * @param remoteFileSeparator the separator.
	 * @return the spec
	 */
	public S remoteFileSeparator(String remoteFileSeparator) {
		this.target.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	/**
	 * Specify a directory path where remote files will be transferred to.
	 * @param localDirectory the localDirectory to set
	 * @return the spec
	 */
	public S localDirectory(File localDirectory) {
		this.target.setLocalDirectory(localDirectory);
		return _this();
	}

	/**
	 * Specify a SpEL expression to evaluate directory path where remote files will be transferred to.
	 * @param localDirectoryExpression the SpEL to determine the local directory.
	 * @return the spec
	 */
	public S localDirectoryExpression(String localDirectoryExpression) {
		return localDirectoryExpression(PARSER.parseExpression(localDirectoryExpression));
	}

	/**
	 * Specify a {@link Function} to evaluate directory path where remote files will be transferred to.
	 * @param localDirectoryFunction the {@link Function} to determine the local directory.
	 * @param <P> the expected payload type.
	 * @return the spec
	 */
	public <P> S localDirectory(Function<Message<P>, String> localDirectoryFunction) {
		return localDirectoryExpression(new FunctionExpression<>(localDirectoryFunction));
	}

	/**
	 * Specify a SpEL expression to evaluate directory path where remote files will be transferred to.
	 * @param localDirectoryExpression a SpEL expression to evaluate the local directory.
	 * @return the Spec.
	 */
	public S localDirectoryExpression(Expression localDirectoryExpression) {
		this.target.setLocalDirectoryExpression(localDirectoryExpression);
		return _this();
	}

	/**
	 * A {@code boolean} flag to identify if local directory should be created automatically.
	 * Defaults to {@code true}.
	 * @param autoCreateLocalDirectory the autoCreateLocalDirectory to set
	 * @return the Spec.
	 */
	public S autoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.target.setAutoCreateLocalDirectory(autoCreateLocalDirectory);
		return _this();
	}

	/**
	 * Set the temporary suffix to use when transferring files to the remote system.
	 * Default {@code .writing}.
	 * @param temporaryFileSuffix the temporaryFileSuffix to set
	 * @return the Spec.
	 */
	public S temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

	/**
	 * Set a {@link FileListFilter} to filter remote files.
	 * @param filter the filter to set
	 * @return the Spec.
	 */
	public S filter(FileListFilter<F> filter) {
		if (this.filter == null) {
			if (filter instanceof CompositeFileListFilter) {
				this.filter = (CompositeFileListFilter<F>) filter;
			}
			else {
				this.filter = new CompositeFileListFilter<>();
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
	 * A {@link FileListFilter} that runs against the <em>local</em> file system view when
	 * using {@code MPUT} command.
	 * @param filter the filter to set
	 * @return the Spec.
	 */
	public S mputFilter(FileListFilter<File> filter) {
		if (this.mputFilter == null) {
			if (filter instanceof CompositeFileListFilter) {
				this.mputFilter = (CompositeFileListFilter<File>) filter;
			}
			else {
				this.mputFilter = new CompositeFileListFilter<>();
				this.mputFilter.addFilter(filter);
			}
			this.target.setMputFilter(this.mputFilter);
		}
		else {
			this.mputFilter.addFilter(filter);
		}
		return _this();
	}

	/**
	 * A {@link SimplePatternFileListFilter} that runs against the <em>local</em> file system view when
	 * using {@code MPUT} command.
	 * @param pattern the {@link SimplePatternFileListFilter} for {@code MPUT} command.
	 * @return the Spec.
	 */
	public S patternMputFilter(String pattern) {
		return mputFilter(new SimplePatternFileListFilter(pattern));
	}

	/**
	 * A {@link SimplePatternFileListFilter} that runs against the <em>local</em> file system view when
	 * using {@code MPUT} command.
	 * @param regex the {@link SimplePatternFileListFilter} for {@code MPUT} command.
	 * @return the Spec.
	 */
	public S regexMpuFilter(String regex) {
		return mputFilter(new RegexPatternFileListFilter(regex));
	}

	/**
	 * Specify a SpEL expression for files renaming during transfer.
	 * @param expression the String in SpEL syntax.
	 * @return the Spec.
	 */
	public S renameExpression(String expression) {
		this.target.setRenameExpressionString(expression);
		return _this();
	}

	/**
	 * Specify a SpEL expression for files renaming during transfer.
	 * @param expression the String in SpEL syntax.
	 * @return the Spec.
	 */
	public S renameExpression(Expression expression) {
		this.target.setRenameExpression(expression);
		return _this();
	}

	/**
	 * Specify a {@link Function} for files renaming during transfer.
	 * @param renameFunction the {@link Function} to use.
	 * @param <P> the expected payload type.
	 * @return the Spec.
	 */
	public <P> S renameFunction(Function<Message<P>, String> renameFunction) {
		this.target.setRenameExpression(new FunctionExpression<>(renameFunction));
		return _this();
	}

	/**
	 * Specify a SpEL expression for local files renaming after downloading.
	 * @param localFilenameExpression the SpEL expression to use.
	 * @return the Spec.
	 */
	public S localFilenameExpression(String localFilenameExpression) {
		return localFilenameExpression(PARSER.parseExpression(localFilenameExpression));
	}

	/**
	 * Specify a {@link Function} for local files renaming after downloading.
	 * @param localFilenameFunction the {@link Function} to use.
	 * @param <P> the expected payload type.
	 * @return the Spec.
	 */
	public <P> S localFilename(Function<Message<P>, String> localFilenameFunction) {
		return localFilenameExpression(new FunctionExpression<>(localFilenameFunction));
	}

	/**
	 * Specify a SpEL expression for local files renaming after downloading.
	 * @param localFilenameExpression a SpEL expression to evaluate the local file name.
	 * @return the Spec.
	 */
	public S localFilenameExpression(Expression localFilenameExpression) {
		this.target.setLocalFilenameGeneratorExpression(localFilenameExpression);
		return _this();
	}

	/**
	 * Set the file permissions after uploading, e.g. 0600 for
	 * owner read/write.
	 * @param chmod the permissions.
	 * @return the current Spec
	 */
	public S chmod(int chmod) {
		this.target.setChmod(chmod);
		return _this();
	}

	/**
	 * Specify a simple pattern to match remote files.
	 * @param pattern the pattern.
	 * @return the spec.
	 * @see org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	public abstract S patternFileNameFilter(String pattern);

	/**
	 * Specify a simple pattern to match remote files.
	 * @param regex the regex pattern.
	 * @return the spec.
	 * @see org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	public abstract S regexFileNameFilter(String regex);

}
