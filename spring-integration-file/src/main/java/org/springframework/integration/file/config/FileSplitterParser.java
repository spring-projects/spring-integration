/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.splitter.FileSplitter;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class FileSplitterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FileSplitter.class);
		builder.addConstructorArgValue(element.getAttribute("iterator"));
		builder.addConstructorArgValue(element.getAttribute("markers"));
		builder.addConstructorArgValue(element.getAttribute("markers-json"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "first-line-as-header");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "discard-channel", "discardChannelName");
		return builder;
	}

}
