/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zookeeper.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.zookeeper.config.LeaderInitiatorFactoryBean;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
public class LeaderListenerParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(LeaderInitiatorFactoryBean.class)
						.addPropertyReference("client", element.getAttribute("client"))
						.addPropertyValue("role", element.getAttribute(IntegrationNamespaceUtils.ROLE))
						.addPropertyValue("path", element.getAttribute("path"));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.PHASE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "candidate");

		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

}
