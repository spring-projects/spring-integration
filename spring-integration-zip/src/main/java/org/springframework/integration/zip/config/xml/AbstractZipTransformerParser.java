/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zip.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Base class for Zip transformer parsers.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 6.1
 */
public abstract class AbstractZipTransformerParser extends AbstractTransformerParser {

	/**
	 * @param element The XML Element to process
	 * @param parserContext The Spring ParserContext
	 * @param builder BeanDefinitionBuilder for constructing Bean Definitions
	 */
	@Override
	protected final void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String deleteFiles = element.getAttribute("delete-files");
		if (StringUtils.hasText(deleteFiles)) {
			builder.addPropertyValue("deleteFiles", deleteFiles);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "result-type", "zipResultType");
		postProcessTransformer(element, parserContext, builder);
	}

	/**
	 * Subclasses may override this method to provide additional configuration.
	 *
	 * @param element The XML Element to process
	 * @param parserContext The Spring ParserContext
	 * @param builder BeanDefinitionBuilder for constructing Bean Definitions
	 */
	protected void postProcessTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
	}

}
