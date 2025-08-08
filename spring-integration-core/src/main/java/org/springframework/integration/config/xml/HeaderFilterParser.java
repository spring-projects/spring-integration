/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.HeaderFilter;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'header-filter' element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class HeaderFilterParser extends AbstractTransformerParser {

	@Override
	protected final String getTransformerClassName() {
		return HeaderFilter.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String headerNames = element.getAttribute("header-names");
		if (!StringUtils.hasText(headerNames)) {
			parserContext.getReaderContext().error("The 'header-names' attribute must not be empty.",
					parserContext.extractSource(element));
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pattern-match");
		builder.addConstructorArgValue(headerNames);
	}

}
