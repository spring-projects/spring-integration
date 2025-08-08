/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Base class for url-based outbound gateway parsers.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractOutboundGatewayParser extends AbstractConsumerEndpointParser {

	protected abstract String getGatewayClassName(Element element);

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getGatewayClassName(element));
		String url = this.parseUrl(element, parserContext);
		builder.addConstructorArgValue(url);
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("replyChannel", replyChannel);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		this.postProcessGateway(builder, element, parserContext);
		return builder;
	}

	protected String parseUrl(Element element, ParserContext parserContext) {
		String url = element.getAttribute("url");
		if (!StringUtils.hasText(url)) {
			parserContext.getReaderContext().error("The 'url' attribute is required.", element);
		}
		return url;
	}

	/**
	 * Subclasses may override this method for additional configuration.
	 * @param builder The builder.
	 * @param element The element.
	 * @param parserContext The parser context.
	 */
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
	}

}
