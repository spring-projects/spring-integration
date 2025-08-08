/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.SyslogToMapTransformer;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class SyslogToMapTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return SyslogToMapTransformer.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// no attributes
	}

}
