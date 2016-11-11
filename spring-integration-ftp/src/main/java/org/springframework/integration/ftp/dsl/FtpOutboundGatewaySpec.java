/*
 * Copyright 2014-2016 the original author or authors.
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

package org.springframework.integration.ftp.dsl;

import org.apache.commons.net.ftp.FTPFile;

import org.springframework.integration.file.dsl.RemoteFileOutboundGatewaySpec;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;

/**
 * A {@link RemoteFileOutboundGatewaySpec} for FTP.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public class FtpOutboundGatewaySpec extends RemoteFileOutboundGatewaySpec<FTPFile, FtpOutboundGatewaySpec> {

	FtpOutboundGatewaySpec(AbstractRemoteFileOutboundGateway<FTPFile> outboundGateway) {
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

}
