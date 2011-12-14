/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.integration.file.config.AbstractRemoteFileInboundChannelAdapterParser;

/**
 * Parser for the FTP 'inbound-channel-adapter' element.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class FtpInboundChannelAdapterParser extends AbstractRemoteFileInboundChannelAdapterParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.ftp";


	@Override
	protected String getMessageSourceClassname() {
		return BASE_PACKAGE + ".inbound.FtpInboundFileSynchronizingMessageSource";
	}

	@Override
	protected String getInboundFileSynchronizerClassname() {
		return BASE_PACKAGE + ".inbound.FtpInboundFileSynchronizerFactoryBean";
	}

	@Override
	protected String getSimplePatternFileListFilterClassname() {
		return BASE_PACKAGE + ".filters.FtpSimplePatternFileListFilter";
	}

	@Override
	protected String getRegexPatternFileListFilterClassname() {
		return BASE_PACKAGE + ".filters.FtpRegexPatternFileListFilter";
	}

}
