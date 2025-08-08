/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.IntegrationManagementConfigurer;

/**
 * Parser for the &lt;management/&gt; element.
 *
 * @author Gary Russell
 * @since 4.2
 */
public class IntegrationManagementParser extends AbstractBeanDefinitionParser {

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		return IntegrationManagementConfigurer.MANAGEMENT_CONFIGURER_NAME;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(IntegrationManagementConfigurer.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-logging-enabled");
		return builder.getBeanDefinition();
	}

	@Override
	protected boolean shouldFireEvents() {
		return false;
	}

}
