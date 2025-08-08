/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.util.StringUtils;

/**
 * Parser for the "stdout-" and "stderr-channel-adapter" elements.
 *
 * @author Mark Fisher
 */
public class ConsoleOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.stream.CharacterStreamWritingMessageHandler");
		if (element.getLocalName().startsWith("stderr")) {
			builder.setFactoryMethod("stderr");
		}
		else {
			builder.setFactoryMethod("stdout");
		}
		String charsetName = element.getAttribute("charset");
		if (StringUtils.hasText(charsetName)) {
			builder.addConstructorArgValue(charsetName);
		}
		if ("true".equals(element.getAttribute("append-newline"))) {
			builder.addPropertyValue("shouldAppendNewLine", Boolean.TRUE);
		}
		return builder.getBeanDefinition();
	}

}
