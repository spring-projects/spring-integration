/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.sftp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.config.RemoteFileOutboundChannelAdapterParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.sftp.outbound.SftpMessageHandler;
import org.springframework.integration.sftp.session.SftpRemoteFileTemplate;

/**
 * Parser for SFTP Outbound Channel Adapters.
 *
 * @author Gary Russell
 * @since 4.1
 *
 */
public class SftpOutboundChannelAdapterParser extends RemoteFileOutboundChannelAdapterParser {

	@Override
	protected Class<?> handlerClass() {
		return SftpMessageHandler.class;
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
