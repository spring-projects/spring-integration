/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.config.AbstractRemoteFileOutboundGatewayParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.ftp.filters.FtpRegexPatternFileListFilter;
import org.springframework.integration.ftp.filters.FtpSimplePatternFileListFilter;
import org.springframework.integration.ftp.gateway.FtpOutboundGateway;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
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

	@Override
	protected void postProcessBuilder(BeanDefinitionBuilder builder, Element element) {
		BeanDefinition templateDefinition = (BeanDefinition) builder.getRawBeanDefinition()
				.getConstructorArgumentValues()
				.getIndexedArgumentValues()
				.values()
				.iterator()
				.next()
				.getValue();
		templateDefinition.getPropertyValues() // NOSONAR never null
				.add("existsMode", FtpRemoteFileTemplate.ExistsMode.NLST);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "working-dir-expression",
				"workingDirExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "chmod", "chmodOctal");
	}

}
