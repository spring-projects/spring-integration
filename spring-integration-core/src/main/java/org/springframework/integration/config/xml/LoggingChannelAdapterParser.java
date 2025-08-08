/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'logging-channel-adapter' element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 1.0.1
 */
public class LoggingChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LoggingHandler.class);
		builder.addConstructorArgValue(element.getAttribute("level"));
		String expression = element.getAttribute("expression");
		String logFullMessage = element.getAttribute("log-full-message");
		if (StringUtils.hasText(logFullMessage)) {
			if (StringUtils.hasText(expression)) {
				parserContext.getReaderContext().error(
						"The 'expression' and 'log-full-message' attributes are mutually exclusive.", source);
			}
			builder.addPropertyValue("shouldLogFullMessage", logFullMessage);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "logger-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expression", "logExpressionString");
		return builder.getBeanDefinition();
	}

}
