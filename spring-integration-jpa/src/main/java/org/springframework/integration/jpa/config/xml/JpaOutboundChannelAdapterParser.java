/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.outbound.JpaOutboundGatewayFactoryBean;

/**
 * The parser for JPA outbound channel adapter.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class JpaOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder jpaOutboundChannelAdapterBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(JpaOutboundGatewayFactoryBean.class);
		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getJpaExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "persist-mode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "flush-size");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "clear-on-flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element, "parameter-source-factory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, element,
				"use-payload-as-parameter-source");

		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String channelAdapterId = resolveId(element, jpaOutboundChannelAdapterBuilder.getRawBeanDefinition(),
				parserContext);
		final String jpaExecutorBeanName = channelAdapterId + ".jpaExecutor";

		parserContext.registerBeanComponent(
				new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		jpaOutboundChannelAdapterBuilder.addPropertyReference("jpaExecutor", jpaExecutorBeanName)
				.addPropertyValue("producesReply", Boolean.FALSE);

		return jpaOutboundChannelAdapterBuilder.getBeanDefinition();

	}

	@Override
	protected boolean isUsingReplyProducer() {
		return true;
	}

}
