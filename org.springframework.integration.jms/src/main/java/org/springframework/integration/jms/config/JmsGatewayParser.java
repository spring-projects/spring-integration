/*
 * Copyright 2002-2008 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.config;

import javax.jms.Session;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;jms-gateway&gt; element.
 * 
 * @author Mark Fisher
 */
public class JmsGatewayParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return JmsGateway.class;
	}

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String messageConverter = element.getAttribute(JmsAdapterParserUtils.MESSAGE_CONVERTER_ATTRIBUTE);
		if (StringUtils.hasText(element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE))) {
			throw new BeanCreationException(JmsGateway.class.getSimpleName() +
					" does not accept a '" + JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE +
					"' reference. One of '" + JmsAdapterParserUtils.DESTINATION_ATTRIBUTE + "' or '" +
					JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE + "' must be provided.");
		}
		if (StringUtils.hasText(destination) || StringUtils.hasText(destinationName)) {
			builder.addPropertyReference(JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
					JmsAdapterParserUtils.determineConnectionFactoryBeanName(element));
			if (StringUtils.hasText(destination)) {
				builder.addPropertyReference(JmsAdapterParserUtils.DESTINATION_PROPERTY, destination);
			}
			else {
				builder.addPropertyValue(JmsAdapterParserUtils.DESTINATION_NAME_PROPERTY, destinationName);
			}
		}
		else {
			throw new BeanCreationException("One of '" + JmsAdapterParserUtils.DESTINATION_ATTRIBUTE +
					"' or '" + JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE + "' must be provided.");
		}
		if (StringUtils.hasText(messageConverter)) {
			builder.addPropertyReference(JmsAdapterParserUtils.MESSAGE_CONVERTER_PROPERTY, messageConverter);
		}
		Integer acknowledgeMode = JmsAdapterParserUtils.parseAcknowledgeMode(element);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == Session.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		String transactionManager = element.getAttribute("transaction-manager");
		if (StringUtils.hasText(transactionManager)) {
			builder.addPropertyReference("transactionManager", transactionManager);
		}
		String requestChannel = element.getAttribute("request-channel");
		if (StringUtils.hasText(requestChannel)) {
			builder.addPropertyReference("requestChannel", requestChannel);
		}
		String requestTimeout = element.getAttribute("request-timeout");
		if (StringUtils.hasText(requestTimeout)) {
			builder.addPropertyValue("requestTimeout", Long.parseLong(requestTimeout));
		}
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("replyChannel", replyChannel);
		}
		String replyTimeout = element.getAttribute("reply-timeout");
		if (StringUtils.hasText(replyTimeout)) {
			builder.addPropertyValue("replyTimeout", Long.parseLong(replyTimeout));
		}
		if ("true".equals(element.getAttribute("expect-reply"))) {
			builder.addPropertyValue("expectReply", Boolean.TRUE);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
	}

}
