/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ServiceActivatorFactoryBean;

/**
 * Parser for the &lt;service-activator&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artme Bilan
 */
public class ServiceActivatorParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return ServiceActivatorFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

	@Override
	void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "not-propagated-headers");
	}

}
