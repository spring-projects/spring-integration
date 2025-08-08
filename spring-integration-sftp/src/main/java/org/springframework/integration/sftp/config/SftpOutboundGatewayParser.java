/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.sftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.config.AbstractRemoteFileOutboundGatewayParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.sftp.filters.SftpRegexPatternFileListFilter;
import org.springframework.integration.sftp.filters.SftpSimplePatternFileListFilter;
import org.springframework.integration.sftp.gateway.SftpOutboundGateway;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 *
 */
public class SftpOutboundGatewayParser extends AbstractRemoteFileOutboundGatewayParser {

	@Override
	public String getGatewayClassName() {
		return SftpOutboundGateway.class.getName();
	}

	@Override
	protected String getSimplePatternFileListFilterClassName() {
		return SftpSimplePatternFileListFilter.class.getName();
	}

	@Override
	protected String getRegexPatternFileListFilterClassName() {
		return SftpRegexPatternFileListFilter.class.getName();
	}

	@Override
	protected Class<? extends RemoteFileOperations<?>> getTemplateClass() {
		return SftpRemoteFileTemplate.class;
	}

	@Override
	protected void postProcessBuilder(BeanDefinitionBuilder builder, Element element) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "chmod", "chmodOctal");
	}

}
