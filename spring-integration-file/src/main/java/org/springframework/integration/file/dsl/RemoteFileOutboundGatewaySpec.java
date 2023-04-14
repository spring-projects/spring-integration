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

import java.io.File;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.ComponentsRegistration;
import org.springframework.integration.dsl.MessageHandlerSpec;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.FileNameGenerator;
import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.ExpressionFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.integration.file.filters.RegexPatternFileListFilter;
import org.springframework.integration.file.filters.SimplePatternFileListFilter;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.file.support.FileExistsMode;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * The {@link MessageHandlerSpec} for the {@link AbstractRemoteFileOutboundGateway}.
 *
 * @param <F> the target file type.
 * @param <S> the target {@link RemoteFileOutboundGatewaySpec} implementation type.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public abstract class RemoteFileOutboundGatewaySpec<F, S extends RemoteFileOutboundGatewaySpec<F, S>>
		extends MessageHandlerSpec<S, AbstractRemoteFileOutboundGateway<F>>
		implements ComponentsRegistration {

	@Nullable
	private CompositeFileListFilter<F> filter;

	@Nullable
	private ExpressionFileListFilter<F> expressionFileListFilter;

	@Nullable
	private ExpressionFileListFilter<File> mputExpressionFileListFilter;

	@Nullable
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
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param expression the SpEL expression for files filtering.
	 * @return the spec.
	 * @see AbstractRemoteFileOutboundGateway#setFilter(FileListFilter)
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
	 * @see AbstractRemoteFileOutboundGateway#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S filterFunction(Function<F, Boolean> filterFunction) {
		this.expressionFileListFilter = new ExpressionFileListFilter<>(new FunctionExpression<>(filterFunction));
		return filter(this.expressionFileListFilter);
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
	public S regexMputFilter(String regex) {
		return mputFilter(new RegexPatternFileListFilter(regex));
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param expression the SpEL expression for files filtering.
	 * @return the spec.
	 * @see AbstractRemoteFileOutboundGateway#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S mputFilterExpression(String expression) {
		this.mputExpressionFileListFilter = new ExpressionFileListFilter<>(expression);
		return mputFilter(this.mputExpressionFileListFilter);
	}

	/**
	 * Configure the {@link ExpressionFileListFilter}.
	 * @param filterFunction the {@link Function} for files filtering.
	 * @return the spec.
	 * @see AbstractRemoteFileOutboundGateway#setFilter(FileListFilter)
	 * @see ExpressionFileListFilter
	 */
	public S mputFilterFunction(Function<File, Boolean> filterFunction) {
		this.mputExpressionFileListFilter = new ExpressionFileListFilter<>(new FunctionExpression<>(filterFunction));
		return mputFilter(this.mputExpressionFileListFilter);
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
	 * @since 5.2
	 */
	public <P> S localFilenameFunction(Function<Message<P>, String> localFilenameFunction) {
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
	 * Determine the action to take when using GET and MGET operations when the file
	 * already exists locally, or PUT and MPUT when the file exists on the remote
	 * system.
	 * @param fileExistsMode the fileExistsMode to set.
	 * @return the current Spec
	 */
	public S fileExistsMode(FileExistsMode fileExistsMode) {
		this.target.setFileExistsMode(fileExistsMode);
		return _this();
	}

	/**
	 * Determine whether the remote directory should automatically be created when
	 * sending files to the remote system.
	 * @param autoCreateDirectory true to create the directory.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setAutoCreateDirectory(boolean)
	 */
	public S autoCreateDirectory(boolean autoCreateDirectory) {
		this.target.setAutoCreateDirectory(autoCreateDirectory);
		return _this();
	}

	/**
	 * Set the remote directory expression used to determine the remote directory to which
	 * files will be sent.
	 * @param remoteDirectoryExpression the remote directory expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 */
	public S remoteDirectoryExpression(String remoteDirectoryExpression) {
		return remoteDirectoryExpression(PARSER.parseExpression(remoteDirectoryExpression));
	}

	/**
	 * Specify a {@link Function} for remote directory.
	 * @param remoteDirectoryFunction the {@link Function} to use.
	 * @param <P> the expected payload type.
	 * @return the Spec.
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 * @see FunctionExpression
	 */
	public <P> S remoteDirectoryFunction(Function<Message<P>, String> remoteDirectoryFunction) {
		return remoteDirectoryExpression(new FunctionExpression<>(remoteDirectoryFunction));
	}

	/**
	 * Set the remote directory expression used to determine the remote directory to which
	 * files will be sent.
	 * @param remoteDirectoryExpression the remote directory expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 */
	public S remoteDirectoryExpression(Expression remoteDirectoryExpression) {
		this.target.setRemoteDirectoryExpression(remoteDirectoryExpression);
		return _this();
	}

	/**
	 * Set a temporary remote directory expression; used when transferring files to the remote
	 * system.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 */
	public S temporaryRemoteDirectoryExpression(String temporaryRemoteDirectoryExpression) {
		return temporaryRemoteDirectoryExpression(PARSER.parseExpression(temporaryRemoteDirectoryExpression));
	}

	/**
	 * Set a temporary remote directory function; used when transferring files to the remote
	 * system.
	 * @param temporaryRemoteDirectoryFunction the file name expression.
	 * @param <P> the expected payload type.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 */
	public <P> S temporaryRemoteDirectoryFunction(Function<Message<P>, String> temporaryRemoteDirectoryFunction) {
		return temporaryRemoteDirectoryExpression(new FunctionExpression<>(temporaryRemoteDirectoryFunction));
	}

	/**
	 * Set a temporary remote directory expression; used when transferring files to the remote
	 * system.
	 * @param temporaryRemoteDirectoryExpression the temporary remote directory expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setRemoteDirectoryExpression
	 */
	public S temporaryRemoteDirectoryExpression(Expression temporaryRemoteDirectoryExpression) {
		this.target.setTemporaryRemoteDirectoryExpression(temporaryRemoteDirectoryExpression);
		return _this();
	}

	/**
	 * Set the file name expression to determine the full path to the remote file.
	 * @param fileNameExpression the file name expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setFileNameExpression
	 */
	public S fileNameExpression(String fileNameExpression) {
		return fileNameExpression(PARSER.parseExpression(fileNameExpression));
	}

	/**
	 * Set the file name function to determine the full path to the remote file.
	 * @param fileNameFunction the file name expression.
	 * @param <P> the expected payload type.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setFileNameExpression
	 */
	public <P> S fileNameFunction(Function<Message<P>, String> fileNameFunction) {
		return fileNameExpression(new FunctionExpression<>(fileNameFunction));
	}

	/**
	 * Set the file name expression to determine the full path to the remote file.
	 * @param fileNameExpression the file name expression.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setFileNameExpression
	 */
	public S fileNameExpression(Expression fileNameExpression) {
		this.target.setFileNameExpression(fileNameExpression);
		return _this();
	}

	/**
	 * Set whether a temporary file name is used when sending files to the remote system.
	 * @param useTemporaryFileName true to use a temporary file name.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setUseTemporaryFileName
	 */
	public S useTemporaryFileName(boolean useTemporaryFileName) {
		this.target.setUseTemporaryFileName(useTemporaryFileName);
		return _this();
	}

	/**
	 * Set the file name generator used to generate the remote filename to be used when transferring
	 * files to the remote system.
	 * @param fileNameGenerator the file name generator.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setFileNameGenerator
	 */
	public S fileNameGenerator(FileNameGenerator fileNameGenerator) {
		this.target.setFileNameGenerator(fileNameGenerator);
		return _this();
	}

	/**
	 * Set the charset to use when converting String payloads to bytes as the content of the
	 * remote file. Default {@code UTF-8}.
	 * @param charset the charset.
	 * @return the current Spec
	 * @since 5.2
	 * @see AbstractRemoteFileOutboundGateway#setCharset
	 */
	public S charset(String charset) {
		this.target.setCharset(charset);
		return _this();
	}

	@Override
	public Map<Object, String> getComponentsToRegister() {
		Map<Object, String> componentsToRegister = new LinkedHashMap<>();
		if (this.expressionFileListFilter != null) {
			componentsToRegister.put(this.expressionFileListFilter, null);
		}
		if (this.mputExpressionFileListFilter != null) {
			componentsToRegister.put(this.mputExpressionFileListFilter, null);
		}

		return componentsToRegister;
	}

	/**
	 * Specify a simple pattern to match remote files (e.g. '*.txt').
	 * @param pattern the pattern.
	 * @return the spec.
	 * @see org.springframework.integration.file.filters.AbstractSimplePatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	public abstract S patternFileNameFilter(String pattern);

	/**
	 * Specify a simple pattern to match remote files (e.g. '[0-9].*.txt').
	 * @param regex the regex pattern.
	 * @return the spec.
	 * @see org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter
	 * @see #filter(org.springframework.integration.file.filters.FileListFilter)
	 */
	public abstract S regexFileNameFilter(String regex);

}
