/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the &lt;mail-to-string-transformer&gt; element.
 *
 * @author Mark Fisher
 */
public class MailToStringTransformerParser extends AbstractTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return "org.springframework.integration.mail.transformer.MailToStringTransformer";
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
	}

}
