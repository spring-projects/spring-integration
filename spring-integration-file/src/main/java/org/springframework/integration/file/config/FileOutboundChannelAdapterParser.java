/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the 'file'
 * namespace.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 */
public class FileOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = FileWritingMessageHandlerBeanDefinitionBuilder.configure(element, false, parserContext);
		return handlerBuilder.getBeanDefinition();
	}

	@Override
	protected boolean isUsingReplyProducer() {
		// cannot be automatically determined by superclass because we are using a factory bean.
		return true;
	}

}
