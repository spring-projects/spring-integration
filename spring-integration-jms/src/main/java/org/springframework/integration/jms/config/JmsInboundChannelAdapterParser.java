/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.JmsDestinationPollingSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element of the 'jms' namespace.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class JmsInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsDestinationPollingSource.class);
		String componentName =  this.resolveId(element, builder.getBeanDefinition(), parserContext);
		if (StringUtils.hasText(componentName)) {
			builder.addPropertyValue("componentName", componentName);
		}
		String jmsTemplate = element.getAttribute(JmsParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String headerMapper = element.getAttribute(JmsParserUtils.HEADER_MAPPER_ATTRIBUTE);
		boolean hasJmsTemplate = StringUtils.hasText(jmsTemplate);
		boolean hasDestinationRef = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		if (hasJmsTemplate) {
			JmsParserUtils.verifyNoJmsTemplateAttributes(element, parserContext);
			builder.addConstructorArgReference(jmsTemplate);
		}
		else {
			builder.addConstructorArgValue(JmsParserUtils.parseJmsTemplateBeanDefinition(element, parserContext));
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "acknowledge", "sessionAcknowledgeMode");
		if (hasDestinationRef || hasDestinationName) {
			if (hasDestinationRef) {
				if (hasDestinationName) {
					parserContext.getReaderContext().error("The 'destination-name' " +
							"and 'destination' attributes are mutually exclusive.", parserContext.extractSource(element));
				}
				builder.addPropertyReference(JmsParserUtils.DESTINATION_PROPERTY, destination);
			}
			else if (hasDestinationName) {
				builder.addPropertyValue(JmsParserUtils.DESTINATION_NAME_PROPERTY, destinationName);
			}
		}
		else if (!hasJmsTemplate) {
			parserContext.getReaderContext().error("either a '" + JmsParserUtils.JMS_TEMPLATE_ATTRIBUTE +
					"' or one of '" + JmsParserUtils.DESTINATION_ATTRIBUTE + "' or '"
					+ JmsParserUtils.DESTINATION_NAME_ATTRIBUTE +
					"' attributes must be provided for a polling JMS adapter", parserContext.extractSource(element));
		}
		if (StringUtils.hasText(headerMapper)) {
			builder.addPropertyReference(JmsParserUtils.HEADER_MAPPER_PROPERTY, headerMapper);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "selector", "messageSelector");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		return builder.getBeanDefinition();
	}

}
