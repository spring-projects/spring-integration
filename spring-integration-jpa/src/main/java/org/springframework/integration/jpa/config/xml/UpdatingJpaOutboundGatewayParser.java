/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.support.OutboundGatewayType;

/**
 * The Parser for Updating JPA Outbound Gateway.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
public class UpdatingJpaOutboundGatewayParser extends AbstractJpaOutboundGatewayParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {
		BeanDefinitionBuilder jpaOutboundGatewayBuilder = super.parseHandler(gatewayElement, parserContext);

		BeanDefinitionBuilder jpaExecutorBuilder =
				JpaParserUtils.getOutboundGatewayJpaExecutorBuilder(gatewayElement, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "persist-mode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "flush-size");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "clear-on-flush");

		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String gatewayId = resolveId(gatewayElement, jpaOutboundGatewayBuilder.getRawBeanDefinition(),
				parserContext);
		final String jpaExecutorBeanName = gatewayId + ".jpaExecutor";

		parserContext.registerBeanComponent(
				new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		return jpaOutboundGatewayBuilder.addPropertyReference("jpaExecutor", jpaExecutorBeanName)
				.addPropertyValue("gatewayType", OutboundGatewayType.UPDATING);
	}

}
