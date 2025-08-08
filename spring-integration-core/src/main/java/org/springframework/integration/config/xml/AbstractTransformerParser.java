/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.MessageTransformingHandler;

/**
 * @author Mark Fisher
 */
public abstract class AbstractTransformerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				MessageTransformingHandler.class);
		BeanDefinitionBuilder transformerBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(this.getTransformerClassName());
		this.parseTransformer(element, parserContext, transformerBuilder);
		String transformerBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				transformerBuilder.getBeanDefinition(), parserContext.getRegistry());
		builder.addConstructorArgReference(transformerBeanName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

	protected abstract String getTransformerClassName();

	protected abstract void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder);

}
