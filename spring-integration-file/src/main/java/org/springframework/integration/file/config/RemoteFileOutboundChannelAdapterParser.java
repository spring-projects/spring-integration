/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.file.remote.RemoteFileOperations;
import org.springframework.integration.file.remote.handler.FileTransferringMessageHandler;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author David Turanski
 * @author Gary Russell
 * @since 2.0
 */
public abstract class RemoteFileOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(handlerClass());

		BeanDefinition templateDefinition = FileParserUtils.parseRemoteFileTemplate(element, parserContext, true,
				getTemplateClass());

		handlerBuilder.addConstructorArgValue(templateDefinition);
		String mode = element.getAttribute("mode");
		if (StringUtils.hasText(mode)) {
			handlerBuilder.addConstructorArgValue(mode);
		}
		postProcessBuilder(handlerBuilder, element);
		return handlerBuilder.getBeanDefinition();
	}

	protected Class<?> handlerClass() {
		return FileTransferringMessageHandler.class;
	}

	protected void postProcessBuilder(BeanDefinitionBuilder builder, Element element) {
		// no-op
	}

	protected abstract Class<? extends RemoteFileOperations<?>> getTemplateClass();

}
