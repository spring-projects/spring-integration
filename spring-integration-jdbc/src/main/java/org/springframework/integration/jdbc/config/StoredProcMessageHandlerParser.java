/*
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
import org.springframework.integration.jdbc.outbound.StoredProcMessageHandler;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
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

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StoredProcMessageHandler.class);

		BeanDefinitionBuilder storedProcExecutorBuilder =
				StoredProcParserUtils.getStoredProcExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element,
				"use-payload-as-parameter-source");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(storedProcExecutorBuilder, element,
				"sql-parameter-source-factory");

		AbstractBeanDefinition storedProcExecutorBuilderBeanDefinition = storedProcExecutorBuilder.getBeanDefinition();
		String messageHandlerId = this.resolveId(element, builder.getRawBeanDefinition(), parserContext);
		String storedProcExecutorBeanName = messageHandlerId + ".storedProcExecutor";

		parserContext.registerBeanComponent(
				new BeanComponentDefinition(storedProcExecutorBuilderBeanDefinition, storedProcExecutorBeanName));

		builder.addConstructorArgReference(storedProcExecutorBeanName);

		return builder.getBeanDefinition();

	}

}
