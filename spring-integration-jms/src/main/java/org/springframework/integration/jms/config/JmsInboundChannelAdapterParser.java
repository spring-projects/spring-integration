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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
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
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.jms.JmsDestinationPollingSource");
		String componentName =  this.resolveId(element, builder.getBeanDefinition(), parserContext);
		if (StringUtils.hasText(componentName)){
			builder.addPropertyValue("componentName", componentName);
		}
		String jmsTemplate = element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String pubSubDomain = element.getAttribute(JmsAdapterParserUtils.PUB_SUB_DOMAIN_ATTRIBUTE);
		String headerMapper = element.getAttribute(JmsAdapterParserUtils.HEADER_MAPPER_ATTRIBUTE);
		boolean hasDestinationRef = StringUtils.hasText(destination);
		boolean hasDestinationName = StringUtils.hasText(destinationName);
		if (StringUtils.hasText(jmsTemplate)) {
			JmsAdapterParserUtils.verifyNoJmsTemplateAttributes(element, parserContext);
			builder.addConstructorArgReference(jmsTemplate);
		}
		else if (hasDestinationRef || hasDestinationName) {
			builder.addConstructorArgReference(JmsAdapterParserUtils.determineConnectionFactoryBeanName(element, parserContext));
			if (hasDestinationRef) {
				if (hasDestinationName) {
					parserContext.getReaderContext().error("The 'destination-name' " +
							"and 'destination' attributes are mutually exclusive.", source);
				}
				builder.addConstructorArgReference(destination);
			}
			else if (hasDestinationName) {
				builder.addConstructorArgValue(destinationName);
				if (StringUtils.hasText(pubSubDomain)) {
					builder.addPropertyValue(JmsAdapterParserUtils.PUB_SUB_DOMAIN_PROPERTY, pubSubDomain);
				}
			}
		}
		else {
			throw new BeanCreationException("either a '" + JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE +
					"' or one of '" + JmsAdapterParserUtils.DESTINATION_ATTRIBUTE + "' or '"
					+ JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE +
					"' attributes must be provided for a polling JMS adapter");
		}
		if (StringUtils.hasText(headerMapper)) {
			builder.addPropertyReference(JmsAdapterParserUtils.HEADER_MAPPER_PROPERTY, headerMapper);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "selector", "messageSelector");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "destination-resolver");
		BeanDefinition beanDefinition = builder.getBeanDefinition();
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
		BeanComponentDefinition component = new BeanComponentDefinition(beanDefinition, beanName); 
		parserContext.registerBeanComponent(component);
		return beanName;
	}

}
