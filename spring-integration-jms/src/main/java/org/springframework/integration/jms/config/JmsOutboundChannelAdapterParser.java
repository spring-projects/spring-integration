/*
 * Copyright 2002-2010 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;outbound-channel-adapter/&gt; element of the jms namespace.
 * 
 * @author Mark Fisher
 */
public class JmsOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.jms.JmsSendingMessageHandler");
		String jmsTemplate = element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String headerMapper = element.getAttribute(JmsAdapterParserUtils.HEADER_MAPPER_ATTRIBUTE);
		boolean hasDestinationRef = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		if (StringUtils.hasText(jmsTemplate)) {
			if (element.hasAttribute(JmsAdapterParserUtils.CONNECTION_FACTORY_ATTRIBUTE) ||
					hasDestinationRef || hasDestinationName) {
				throw new BeanCreationException("When providing a 'jms-template' reference, none of " +
						"'connection-factory', 'destination', or 'destination-name' should be provided.");
			}
			builder.addPropertyReference(JmsAdapterParserUtils.JMS_TEMPLATE_PROPERTY, jmsTemplate);
		}
		else if (hasDestinationRef ^ hasDestinationName) {
			builder.addPropertyReference(JmsAdapterParserUtils.CONNECTION_FACTORY_PROPERTY,
					JmsAdapterParserUtils.determineConnectionFactoryBeanName(element, parserContext));
			if (StringUtils.hasText(destination)) {
				builder.addPropertyReference(JmsAdapterParserUtils.DESTINATION_PROPERTY, destination);
			}
			else {
				builder.addPropertyValue(JmsAdapterParserUtils.DESTINATION_NAME_PROPERTY, destinationName);
				IntegrationNamespaceUtils.setValueIfAttributeDefined(
						builder, element, JmsAdapterParserUtils.PUB_SUB_DOMAIN_ATTRIBUTE);
			}
		}
		else {
			throw new BeanCreationException("Either a 'jms-template' reference " +
			"or one of 'destination' or 'destination-name' must be provided.");
		}
		if (StringUtils.hasText(headerMapper)) {
			builder.addPropertyReference(JmsAdapterParserUtils.HEADER_MAPPER_PROPERTY, headerMapper);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "time-to-live");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "priority");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delivery-persistent");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "explicit-qos-enabled");
		return builder.getBeanDefinition();
	}

}
