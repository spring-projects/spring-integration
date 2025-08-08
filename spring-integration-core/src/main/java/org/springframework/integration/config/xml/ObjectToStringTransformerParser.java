/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.ObjectToStringTransformer;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'object-to-string-transformer' element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ObjectToStringTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return ObjectToStringTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String charset = element.getAttribute("charset");
		if (StringUtils.hasText(charset)) {
			builder.addConstructorArgValue(charset);
		}
	}

}
