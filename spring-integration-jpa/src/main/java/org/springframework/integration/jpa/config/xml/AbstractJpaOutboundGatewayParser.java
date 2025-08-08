/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.outbound.JpaOutboundGatewayFactoryBean;
import org.springframework.util.StringUtils;

/**
 * The Abstract Parser for the JPA Outbound Gateways.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 *
 * @see RetrievingJpaOutboundGatewayParser
 * @see UpdatingJpaOutboundGatewayParser
 *
 */
public abstract class AbstractJpaOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {
		final BeanDefinitionBuilder jpaOutboundGatewayBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(JpaOutboundGatewayFactoryBean.class);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "reply-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "requires-reply");

		String replyChannel = gatewayElement.getAttribute("reply-channel");

		if (StringUtils.hasText(replyChannel)) {
			jpaOutboundGatewayBuilder.addPropertyReference("outputChannel", replyChannel);
		}

		return jpaOutboundGatewayBuilder;

	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

}
