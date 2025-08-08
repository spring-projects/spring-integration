/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.StoredProcMessageHandler;

/**
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class StoredProcMessageHandlerParser extends AbstractOutboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StoredProcMessageHandler.class);

		final BeanDefinitionBuilder storedProcExecutorBuilder = StoredProcParserUtils.getStoredProcExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "use-payload-as-parameter-source");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(storedProcExecutorBuilder, element, "sql-parameter-source-factory");

		final AbstractBeanDefinition storedProcExecutorBuilderBeanDefinition = storedProcExecutorBuilder.getBeanDefinition();
		final String messageHandlerId = this.resolveId(element, builder.getRawBeanDefinition(), parserContext);
		final String storedProcExecutorBeanName = messageHandlerId + ".storedProcExecutor";

		parserContext.registerBeanComponent(new BeanComponentDefinition(storedProcExecutorBuilderBeanDefinition, storedProcExecutorBeanName));

		builder.addConstructorArgReference(storedProcExecutorBeanName);

		return builder.getBeanDefinition();

	}

}
