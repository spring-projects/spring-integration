/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the &lt;groovy:control-bus/&gt; element.
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @since 2.0
 */
public class GroovyControlBusParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(GroovyControlBusFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "customizer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "order");
		return builder;
	}

}
