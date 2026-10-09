/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.inbound.StoredProcPollingChannelAdapter;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 *
 */
public class StoredProcPollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(StoredProcPollingChannelAdapter.class);

		BeanDefinitionBuilder storedProcExecutorBuilder =
				StoredProcParserUtils.getStoredProcExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element,
				"return-value-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "is-function");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element,
				"skip-undeclared-results");

		ManagedMap<String, BeanMetadataElement> returningResultsetMap =
				StoredProcParserUtils.getReturningResultsetBeanDefinitions(element, parserContext);

		if (!returningResultsetMap.isEmpty()) {
			storedProcExecutorBuilder.addPropertyValue("returningResultSetRowMappers", returningResultsetMap);
		}

		AbstractBeanDefinition storedProcExecutorBuilderBeanDefinition = storedProcExecutorBuilder.getBeanDefinition();
		String storedProcPollingChannelAdapterId = resolveId(element, builder.getRawBeanDefinition(), parserContext);
		String storedProcExecutorBeanName = storedProcPollingChannelAdapterId + ".storedProcExecutor";

		parserContext.registerBeanComponent(
				new BeanComponentDefinition(storedProcExecutorBuilderBeanDefinition, storedProcExecutorBeanName));

		builder.addConstructorArgReference(storedProcExecutorBeanName);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");

		return builder.getBeanDefinition();

	}

}
