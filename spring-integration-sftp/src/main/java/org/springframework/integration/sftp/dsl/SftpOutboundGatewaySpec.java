/*
 * Copyright 2014-2024 the original author or authors.
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

package org.springframework.integration.sftp.dsl;

import org.apache.sshd.sftp.client.SftpClient;

import org.springframework.integration.file.dsl.RemoteFileOutboundGatewaySpec;
import org.springframework.integration.file.remote.gateway.AbstractRemoteFileOutboundGateway;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class SftpOutboundGatewaySpec
		extends RemoteFileOutboundGatewaySpec<SftpClient.DirEntry, SftpOutboundGatewaySpec> {

	protected SftpOutboundGatewaySpec(AbstractRemoteFileOutboundGateway<SftpClient.DirEntry> outboundGateway) {
		super(outboundGateway);
	}

	/**
	 * @see SftpSimplePatternFileListFilter
	 */
	@Override
	public SftpOutboundGatewaySpec patternFileNameFilter(String pattern) {
		return filter(new SftpSimplePatternFileListFilter(pattern));
	}

	/**
	 * @see SftpRegexPatternFileListFilter
	 */
	@Override
	public SftpOutboundGatewaySpec regexFileNameFilter(String regex) {
		return filter(new SftpRegexPatternFileListFilter(regex));
	}

}
