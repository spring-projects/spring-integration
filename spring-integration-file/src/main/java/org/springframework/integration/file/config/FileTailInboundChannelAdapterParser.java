/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ali Shahbour
 * @since 3.0
 *
 */
public class FileTailInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(FileTailInboundChannelAdapterFactoryBean.class);

		if (StringUtils.hasText(channelName)) {
			builder.addPropertyReference("outputChannel", channelName);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "native-options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "enable-status-reader");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-event-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-scheduler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delay");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "file-delay");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "end");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reopen");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");

		return builder.getBeanDefinition();
	}

}
