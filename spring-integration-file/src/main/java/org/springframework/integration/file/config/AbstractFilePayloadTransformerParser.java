/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.util.StringUtils;

/**
 * Base class for File payload transformer parsers.
 *
 * @author Mark Fisher
 */
public abstract class AbstractFilePayloadTransformerParser extends AbstractTransformerParser {

	@Override
	protected final void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String deleteFiles = element.getAttribute("delete-files");
		if (StringUtils.hasText(deleteFiles)) {
			builder.addPropertyValue("deleteFiles", deleteFiles);
		}
		this.postProcessTransformer(element, parserContext, builder);
	}

	/**
	 * Subclasses may override this method to provide additional configuration.
	 *
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @param builder The builder.
	 */
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

}
