/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ObjectToMapTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mauro Franceschini
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ObjectToMapTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ObjectToMapTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String objectMapper = element.getAttribute("object-mapper");
		if (StringUtils.hasText(objectMapper)) {
			builder.addConstructorArgReference(objectMapper);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "flatten", "shouldFlattenKeys");
	}

}
