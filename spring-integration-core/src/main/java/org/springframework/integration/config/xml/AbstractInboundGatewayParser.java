/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Base class for inbound gateway parsers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public abstract class AbstractInboundGatewayParser extends AbstractSimpleBeanDefinitionParser {

	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = super.resolveId(element, definition, parserContext);
		if (!StringUtils.hasText(id)) {
			id = element.getAttribute("name");
		}
		if (!StringUtils.hasText(id)) {
			id = parserContext.getReaderContext().generateBeanName(definition);
		}
		return id;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !attributeName.equals("name") && !attributeName.equals("request-channel") // NOSONAR boolean complexity
				&& !attributeName.equals("error-channel")
				&& !attributeName.equals("reply-channel") && super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);
		AbstractBeanDefinition beanDefinition = builder.getRawBeanDefinition();
		beanDefinition.setResource(parserContext.getReaderContext().getResource());
		beanDefinition.setSource(IntegrationNamespaceUtils.createElementDescription(element));
	}

	@Override
	protected final void postProcess(BeanDefinitionBuilder builder, Element element) {
		String requestChannelRef = element.getAttribute("request-channel");
		Assert.hasText(requestChannelRef, "a 'request-channel' reference is required");
		builder.addPropertyValue("requestChannelName", requestChannelRef);
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyValue("replyChannelName", replyChannel);
		}
		String errorChannel = element.getAttribute("error-channel");
		if (StringUtils.hasText(errorChannel)) {
			builder.addPropertyValue("errorChannelName", errorChannel);
		}
		doPostProcess(builder, element);
	}

	/**
	 * Subclasses may add to the bean definition by overriding this method.
	 * @param builder The builder.
	 * @param element The element.
	 */
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
	}

}
