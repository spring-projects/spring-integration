/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.json.JsonToObjectTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JsonToObjectTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return JsonToObjectTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String type = element.getAttribute("type");
		String objectMapper = element.getAttribute("object-mapper");
		if (StringUtils.hasText(type)) {
			builder.addConstructorArgValue(type);
		}
		if (StringUtils.hasText(objectMapper)) {
			builder.addConstructorArgReference(objectMapper);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "value-type-expression",
				"valueTypeExpressionString");

	}

}
