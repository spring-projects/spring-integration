/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for {@code <int:barrier/>}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class BarrierParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(BarrierMessageHandler.class);
		handlerBuilder.addConstructorArgValue(element.getAttribute("timeout"));
		String triggerTimeout = element.getAttribute("trigger-timeout");
		if (StringUtils.hasText(triggerTimeout)) {
			handlerBuilder.addConstructorArgValue(triggerTimeout);
		}
		String processor = element.getAttribute("output-processor");
		if (StringUtils.hasText(processor)) {
			handlerBuilder.addConstructorArgReference(processor);
		}
		IntegrationNamespaceUtils.injectConstructorWithAdapter("correlation-strategy",
				"correlation-strategy-method", "correlation-strategy-expression",
				"CorrelationStrategy", element, handlerBuilder, null, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "requires-reply");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "discard-channel");
		return handlerBuilder;
	}

}

