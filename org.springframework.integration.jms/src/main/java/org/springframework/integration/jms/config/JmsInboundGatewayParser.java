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
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jms.ChannelPublishingJmsMessageListener;
import org.springframework.integration.jms.JmsInboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;inbound-gateway&gt; element of the 'jms' integration namespace.
 * 
 * @author Mark Fisher
 */
public class JmsInboundGatewayParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return JmsInboundGateway.class;
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
		String listenerBeanName = this.parseMessageListener(element, parserContext);
		builder.addConstructorArgReference(listenerBeanName);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
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
		Integer acknowledgeMode = JmsAdapterParserUtils.parseAcknowledgeMode(element);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == Session.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "transaction-manager");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "pub-sub-domain");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-concurrent-consumers");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-messages-per-task");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "idle-task-execution-limit");
	}

	private String parseMessageListener(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ChannelPublishingJmsMessageListener.class);
		builder.addPropertyValue("expectReply", true);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-request-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-reply-payload");		
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
