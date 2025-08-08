/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.file.transformer.FileToStringTransformer;

/**
 * Parser for the &lt;file-to-string-transformer&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FileToStringTransformerParser extends AbstractFilePayloadTransformerParser {

	@Override
	protected String getTransformerClassName() {
		return FileToStringTransformer.class.getName();
	}

	@Override
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
	}

}
