/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
