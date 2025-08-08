/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class IntegrationXmlNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("marshalling-transformer", new MarshallingTransformerParser());
		registerBeanDefinitionParser("unmarshalling-transformer", new UnmarshallingTransformerParser());
		registerBeanDefinitionParser("xslt-transformer", new XsltPayloadTransformerParser());
		registerBeanDefinitionParser("xpath-transformer", new XPathTransformerParser());
		registerBeanDefinitionParser("xpath-header-enricher", new XPathHeaderEnricherParser());
		registerBeanDefinitionParser("xpath-router", new XPathRouterParser());
		registerBeanDefinitionParser("xpath-filter", new XPathFilterParser());
		registerBeanDefinitionParser("xpath-expression", new XPathExpressionParser());
		registerBeanDefinitionParser("xpath-splitter", new XPathMessageSplitterParser());
		registerBeanDefinitionParser("validating-filter", new XmlPayloadValidatingFilterParser());
	}

}
