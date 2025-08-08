/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.smb.config;

import org.springframework.integration.file.config.AbstractRemoteFileOutboundGatewayParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.smb.filters.SmbRegexPatternFileListFilter;
import org.springframework.integration.smb.filters.SmbSimplePatternFileListFilter;
import org.springframework.integration.smb.outbound.SmbOutboundGateway;
import org.springframework.integration.smb.session.SmbRemoteFileTemplate;

/**
 * Parser for the SMB 'outbound-gateway' element.
 *
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbOutboundGatewayParser extends AbstractRemoteFileOutboundGatewayParser {

	@Override
	public String getGatewayClassName() {
		return SmbOutboundGateway.class.getName();
	}

	@Override
	protected String getSimplePatternFileListFilterClassName() {
		return SmbSimplePatternFileListFilter.class.getName();
	}

	@Override
	protected String getRegexPatternFileListFilterClassName() {
		return SmbRegexPatternFileListFilter.class.getName();
	}

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return SmbRemoteFileTemplate.class;
	}

}
