/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class MarshallingTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return "org.springframework.integration.xml.transformer.MarshallingTransformer";
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String resultTransformer = element.getAttribute("result-transformer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-type", "resultType");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-factory", "resultFactoryName");
		String marshaller = element.getAttribute("marshaller");
		Assert.hasText(marshaller, "the 'marshaller' attribute is required");
		builder.addConstructorArgReference(marshaller);
		if (StringUtils.hasText(resultTransformer)) {
			builder.addConstructorArgReference(resultTransformer);
		}
		String extractPayload = element.getAttribute("extract-payload");
		if (StringUtils.hasText(extractPayload)) {
			builder.addPropertyValue("extractPayload", extractPayload);
		}
	}

}
