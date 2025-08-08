/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.stream.CharacterStreamReadingMessageSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;stdin-channel-adapter&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class ConsoleInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				CharacterStreamReadingMessageSource.class);
		String pipe = element.getAttribute("detect-eof");
		if (StringUtils.hasText(pipe)) {
			builder.setFactoryMethod("stdinPipe");
		}
		else {
			builder.setFactoryMethod("stdin");
		}
		String charsetName = element.getAttribute("charset");
		if (StringUtils.hasText(charsetName)) {
			builder.addConstructorArgValue(charsetName);
		}
		return builder.getBeanDefinition();
	}

}
