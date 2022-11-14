/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.integration.smb.dsl;

import jcifs.smb.SmbFile;

import org.springframework.integration.file.dsl.RemoteFileOutboundGatewaySpec;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.outbound.SmbOutboundGateway;

/**
 * A {@link RemoteFileOutboundGatewaySpec} for SMB.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbOutboundGatewaySpec extends RemoteFileOutboundGatewaySpec<SmbFile, SmbOutboundGatewaySpec> {

	protected SmbOutboundGatewaySpec(SmbOutboundGateway outboundGateway) {
		super(outboundGateway);
	}

	/**
	 * @see SmbSimplePatternFileListFilter
	 */
	@Override
	public SmbOutboundGatewaySpec patternFileNameFilter(String pattern) {
		return filter(new SmbSimplePatternFileListFilter(pattern));
	}

	/**
	 * @see SmbRegexPatternFileListFilter
	 */
	@Override
	public SmbOutboundGatewaySpec regexFileNameFilter(String regex) {
		return filter(new SmbRegexPatternFileListFilter(regex));
	}

}
