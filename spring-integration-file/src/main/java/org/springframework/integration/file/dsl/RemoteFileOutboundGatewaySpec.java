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
import org.springframework.util.Assert;

/**
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

	public S options(String options) {
		this.target.setOptions(options);
		return _this();
	}

	public S options(AbstractRemoteFileOutboundGateway.Option... options) {
		Assert.noNullElements(options);
		StringBuilder optionsString = new StringBuilder();
		for (AbstractRemoteFileOutboundGateway.Option option : options) {
			optionsString.append(option.getOption()).append(" ");
		}
		this.target.setOptions(optionsString.toString());
		return _this();
	}

	public S remoteFileSeparator(String remoteFileSeparator) {
		this.target.setRemoteFileSeparator(remoteFileSeparator);
		return _this();
	}

	public S localDirectory(File localDirectory) {
		this.target.setLocalDirectory(localDirectory);
		return _this();
	}

	public S localDirectoryExpression(String localDirectoryExpression) {
		return localDirectoryExpression(PARSER.parseExpression(localDirectoryExpression));
	}

	public <P> S localDirectory(Function<Message<P>, String> localDirectoryFunction) {
		return localDirectoryExpression(new FunctionExpression<>(localDirectoryFunction));
	}

	/**
	 * @param localDirectoryExpression a SpEL expression to evaluate the local directory.
	 * @return the Spec.
	 */
	public S localDirectoryExpression(Expression localDirectoryExpression) {
		this.target.setLocalDirectoryExpression(localDirectoryExpression);
		return _this();
	}

	public S autoCreateLocalDirectory(boolean autoCreateLocalDirectory) {
		this.target.setAutoCreateLocalDirectory(autoCreateLocalDirectory);
		return _this();
	}

	public S temporaryFileSuffix(String temporaryFileSuffix) {
		this.target.setTemporaryFileSuffix(temporaryFileSuffix);
		return _this();
	}

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

	public S patternMputFilter(String pattern) {
		return mputFilter(new SimplePatternFileListFilter(pattern));
	}


	public S regexMpuFilter(String regex) {
		return mputFilter(new RegexPatternFileListFilter(regex));
	}

	public S renameExpression(String expression) {
		this.target.setRenameExpressionString(expression);
		return _this();
	}

	public S localFilenameExpression(String localFilenameExpression) {
		return localFilenameExpression(PARSER.parseExpression(localFilenameExpression));
	}

	public <P> S localFilename(Function<Message<P>, String> localFilenameFunction) {
		return localFilenameExpression(new FunctionExpression<>(localFilenameFunction));
	}

	/**
	 * @param localFilenameExpression a SpEL expression to evaluate the local file name.
	 * @return the Spec.
	 */
	public S localFilenameExpression(Expression localFilenameExpression) {
		this.target.setLocalFilenameGeneratorExpression(localFilenameExpression);
		return _this();
	}

	public abstract S patternFileNameFilter(String pattern);

	public abstract S regexFileNameFilter(String regex);

}
