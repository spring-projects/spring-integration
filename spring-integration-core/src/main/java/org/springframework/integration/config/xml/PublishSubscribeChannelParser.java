/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;publish-subscribe-channel&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PublishSubscribeChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PublishSubscribeChannel.class);
		String taskExecutorRef = element.getAttribute("task-executor");
		if (StringUtils.hasText(taskExecutorRef)) {
			builder.addConstructorArgReference(taskExecutorRef);
		}
		builder.addConstructorArgValue(element.getAttribute("require-subscribers"));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-failures");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-subscribers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "min-subscribers");
		return builder;
	}

}
