/*
 * Copyright 2002-2014 the original author or authors.
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
package org.springframework.integration.ftp.config;

import org.springframework.integration.file.config.AbstractRemoteFileOutboundGatewayParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.gateway.FtpOutboundGateway;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 *
 */
public class FtpOutboundGatewayParser extends AbstractRemoteFileOutboundGatewayParser {

	@Override
	public String getGatewayClassName() {
		return FtpOutboundGateway.class.getName();
	}

	@Override
	protected String getSimplePatternFileListFilterClassName() {
		return FtpSimplePatternFileListFilter.class.getName();
	}

	@Override
	protected String getRegexPatternFileListFilterClassName() {
		return FtpRegexPatternFileListFilter.class.getName();
	}

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return FtpRemoteFileTemplate.class;
	}

}
