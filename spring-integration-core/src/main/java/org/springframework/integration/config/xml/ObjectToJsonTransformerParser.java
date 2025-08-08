/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.json.ObjectToJsonTransformer;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ObjectToJsonTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ObjectToJsonTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String objectMapper = element.getAttribute("object-mapper");
		if (StringUtils.hasText(objectMapper)) {
			builder.addConstructorArgReference(objectMapper);
		}
		String resultType = element.getAttribute("result-type");
		if (StringUtils.hasText(resultType)) {
			builder.addConstructorArgValue(resultType);
		}
		if (element.hasAttribute("content-type")) {
			builder.addPropertyValue("contentType", element.getAttribute("content-type"));
		}
	}

}
