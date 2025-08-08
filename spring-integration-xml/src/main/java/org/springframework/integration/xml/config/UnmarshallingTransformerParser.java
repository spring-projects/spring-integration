/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.util.Assert;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class UnmarshallingTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return "org.springframework.integration.xml.transformer.UnmarshallingTransformer";
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String unmarshaller = element.getAttribute("unmarshaller");
		Assert.hasText(unmarshaller, "the 'unmarshaller' attribute is required");
		builder.addConstructorArgReference(unmarshaller);
	}

}
