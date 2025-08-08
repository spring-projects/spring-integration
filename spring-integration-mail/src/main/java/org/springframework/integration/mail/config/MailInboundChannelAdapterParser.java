/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mail.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mail.MailReceivingMessageSource;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter&gt; element of Spring Integration's 'mail' namespace.
 *
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MailInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MailReceivingMessageSource.class);
		builder.addConstructorArgValue(this.parseMailReceiver(element, parserContext));
		return builder.getBeanDefinition();
	}

	private BeanDefinition parseMailReceiver(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder receiverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				MailReceiverFactoryBean.class);
		Object source = parserContext.extractSource(element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "store-uri");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "protocol");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "search-term-strategy");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "user-flag");
		String session = element.getAttribute("session");
		if (StringUtils.hasText(session)) {
			if (element.hasAttribute("java-mail-properties") || element.hasAttribute("authenticator")) {
				parserContext.getReaderContext().error("Neither 'java-mail-properties' nor 'authenticator' " +
						"references are allowed when a 'session' reference has been provided.", source);
			}
			receiverBuilder.addPropertyReference("session", session);
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "java-mail-properties");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "authenticator");
		}
		String maxFetchSize = element.getAttribute("max-fetch-size");
		if (StringUtils.hasText(maxFetchSize)) {
			receiverBuilder.addPropertyValue("maxFetchSize", maxFetchSize);
		}
		else {
			Element pollerElement = DomUtils.getChildElementByTagName(element, "poller");
			if (pollerElement != null) {
				String mmpp = pollerElement.getAttribute("max-messages-per-poll");
				if (StringUtils.hasText(mmpp)) {
					receiverBuilder.addPropertyValue("maxFetchSize", mmpp);
				}
			}
		}
		receiverBuilder.addPropertyValue("shouldDeleteMessages", element.getAttribute("should-delete-messages"));
		String markAsRead = element.getAttribute("should-mark-messages-as-read");
		if (StringUtils.hasText(markAsRead)) {
			receiverBuilder.addPropertyValue("shouldMarkMessagesAsRead", markAsRead);
		}

		String selectorExpression = element.getAttribute("mail-filter-expression");

		if (StringUtils.hasText(selectorExpression)) {
			RootBeanDefinition expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(selectorExpression);
			receiverBuilder.addPropertyValue("selectorExpression", expressionDef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(receiverBuilder, element, "header-mapper");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "embedded-parts-as-bytes");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "simple-content");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(receiverBuilder, element, "auto-close-folder");

		return receiverBuilder.getBeanDefinition();
	}

}
