/*
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

import java.util.Objects;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.config.RemoteFileOutboundChannelAdapterParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.ftp.outbound.FtpMessageHandler;
import org.springframework.integration.ftp.session.FtpRemoteFileTemplate;

/**
 * Parser for FTP Outbound Channel Adapters.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.1
 *
 */
public class FtpOutboundChannelAdapterParser extends RemoteFileOutboundChannelAdapterParser {

	@Override
	protected Class<?> handlerClass() {
		return FtpMessageHandler.class;
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

		Objects.requireNonNull(templateDefinition).getPropertyValues()
				.add("existsMode", FtpRemoteFileTemplate.ExistsMode.NLST);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "chmod", "chmodOctal");
	}

}
