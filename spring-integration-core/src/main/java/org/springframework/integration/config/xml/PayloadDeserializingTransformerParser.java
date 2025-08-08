/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.PayloadDeserializingTransformer;

/**
 * Parser for the 'payload-deserializing-transformer' element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class PayloadDeserializingTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return PayloadDeserializingTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "deserializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "allow-list", "allowedPatterns");
	}

}
