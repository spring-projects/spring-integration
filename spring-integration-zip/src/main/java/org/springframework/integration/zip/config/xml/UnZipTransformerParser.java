/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zip.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.zip.transformer.UnZipTransformer;

/**
 * Parser for the 'unzip-transformer' element.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
public class UnZipTransformerParser extends AbstractZipTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return UnZipTransformer.class.getName();
	}

	@Override
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");
	}

}
