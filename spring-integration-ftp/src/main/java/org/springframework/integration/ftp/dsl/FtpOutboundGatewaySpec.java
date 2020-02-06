/*
 * Copyright 2014-2020 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import java.util.function.Function;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.file.dsl.RemoteFileOutboundGatewaySpec;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.gateway.FtpOutboundGateway;
import org.springframework.messaging.Message;

/**
 * A {@link RemoteFileOutboundGatewaySpec} for FTP.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class FtpOutboundGatewaySpec extends RemoteFileOutboundGatewaySpec<FTPFile, FtpOutboundGatewaySpec> {

	protected FtpOutboundGatewaySpec(FtpOutboundGateway outboundGateway) {
		super(outboundGateway);
	}

	/**
	 * @see FtpSimplePatternFileListFilter
	 */
	@Override
	public FtpOutboundGatewaySpec patternFileNameFilter(String pattern) {
		return filter(new FtpSimplePatternFileListFilter(pattern));
	}

	/**
	 * @see FtpRegexPatternFileListFilter
	 */
	@Override
	public FtpOutboundGatewaySpec regexFileNameFilter(String regex) {
		return filter(new FtpRegexPatternFileListFilter(regex));
	}

	/**
	 * Specify a SpEL {@link Expression} to evaluate FTP client working directory
	 * against request message.
	 * @param workingDirExpression the SpEL expression to evaluate working directory
	 * @return the spec
	 * @see FtpOutboundGateway#setWorkingDirExpression(Expression)
	 */
	public FtpOutboundGatewaySpec workingDirExpression(String workingDirExpression) {
		((FtpOutboundGateway) this.target).setWorkingDirExpressionString(workingDirExpression);
		return this;
	}

	/**
	 * Specify a SpEL {@link Expression} to evaluate FTP client working directory
	 * against request message.
	 * @param workingDirExpression the SpEL expression to evaluate working directory
	 * @return the spec
	 * @see FtpOutboundGateway#setWorkingDirExpression(Expression)
	 */
	public FtpOutboundGatewaySpec workingDirExpression(Expression workingDirExpression) {
		((FtpOutboundGateway) this.target).setWorkingDirExpression(workingDirExpression);
		return this;
	}

	/**
	 * Specify a {@link Function} to evaluate FTP client working directory
	 * against request message.
	 * @param workingDirFunction the function to evaluate working directory
	 * @return the spec
	 * @see FtpOutboundGateway#setWorkingDirExpression(Expression)
	 */
	public FtpOutboundGatewaySpec workingDirFunction(Function<Message<?>, String> workingDirFunction) {
		((FtpOutboundGateway) this.target).setWorkingDirExpression(new FunctionExpression<>(workingDirFunction));
		return this;
	}

}
