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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element of the 'jms' namespace. 
 * 
 * @author Mark Fisher
 */
public class JmsInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsDestinationPollingSource.class);
		String jmsTemplate = element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String headerMapper = element.getAttribute(JmsAdapterParserUtils.HEADER_MAPPER_ATTRIBUTE);
		if (StringUtils.hasText(jmsTemplate)) {
			if (element.hasAttribute(JmsAdapterParserUtils.CONNECTION_FACTORY_ATTRIBUTE) ||
					element.hasAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE) ||
					element.hasAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE)) {
				throw new BeanCreationException(
						"When providing '" + JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE +
						"', none of '" + JmsAdapterParserUtils.CONNECTION_FACTORY_ATTRIBUTE +
						"', '" + JmsAdapterParserUtils.DESTINATION_ATTRIBUTE + "', or '" +
						JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE + "' should be provided.");
			}
			builder.addConstructorArgReference(jmsTemplate);
		}
		else if (StringUtils.hasText(destination) || StringUtils.hasText(destinationName)) {
			builder.addConstructorArgReference(JmsAdapterParserUtils.determineConnectionFactoryBeanName(element));
			if (StringUtils.hasText(destination)) {
				builder.addConstructorArgReference(destination);
			}
			else if (StringUtils.hasText(destinationName)) {
				builder.addConstructorArgValue(destinationName);
			}
		}
		else {
			throw new BeanCreationException("either a '" + JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE + "' or one of '" +
					JmsAdapterParserUtils.DESTINATION_ATTRIBUTE + "' or '" + JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE +
					"' attributes must be provided for a polling JMS adapter");
		}
		if (StringUtils.hasText(headerMapper)) {
			builder.addPropertyReference(JmsAdapterParserUtils.HEADER_MAPPER_PROPERTY, headerMapper);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
