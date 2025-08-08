/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
