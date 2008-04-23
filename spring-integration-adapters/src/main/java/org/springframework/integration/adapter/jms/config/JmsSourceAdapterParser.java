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

package org.springframework.integration.adapter.jms.config;

import javax.jms.Session;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter;
import org.springframework.integration.adapter.jms.JmsPollableSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;jms-source/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class JmsSourceAdapterParser extends AbstractBeanDefinitionParser {

	private static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";

	private static final String MESSAGE_CONVERTER_PROPERTY = "messageConverter";

	private static final String ACKNOWLEDGE_ATTRIBUTE = "acknowledge";

	private static final String ACKNOWLEDGE_AUTO = "auto";

	private static final String ACKNOWLEDGE_CLIENT = "client";

	private static final String ACKNOWLEDGE_DUPS_OK = "dups-ok";

	private static final String ACKNOWLEDGE_TRANSACTED = "transacted";


	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		if ("true".equals(element.getAttribute("message-driven"))) {
			return parseMessageDrivenSource(element, parserContext);
		}
		return parsePollableSource(element, parserContext);
	}

	private AbstractBeanDefinition parsePollableSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsPollableSource.class);
		if (StringUtils.hasText(element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE))) {
			throw new BeanCreationException(
					"The '" + MESSAGE_CONVERTER_ATTRIBUTE + "' attribute is not supported for a polling JMS adapter. " +
					". Consider providing a '" + JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE + 
					"' reference where the template contains a 'messageConverter' property instead.");
		}
		String jmsTemplate = element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
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
		return builder.getBeanDefinition();
	}

	private AbstractBeanDefinition parseMessageDrivenSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JmsMessageDrivenSourceAdapter.class);
		String destination = element.getAttribute(JmsAdapterParserUtils.DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(JmsAdapterParserUtils.DESTINATION_NAME_ATTRIBUTE);
		String messageConverter = element.getAttribute(MESSAGE_CONVERTER_ATTRIBUTE);
		if (StringUtils.hasText(element.getAttribute(JmsAdapterParserUtils.JMS_TEMPLATE_ATTRIBUTE))) {
			throw new BeanCreationException(JmsMessageDrivenSourceAdapter.class.getSimpleName() +
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
			builder.addPropertyReference(MESSAGE_CONVERTER_PROPERTY, messageConverter);
		}
		Integer acknowledgeMode = parseAcknowledgeMode(element);
		if (acknowledgeMode != null) {
			if (acknowledgeMode.intValue() == Session.SESSION_TRANSACTED) {
				builder.addPropertyValue("sessionTransacted", Boolean.TRUE);
			}
			else {
				builder.addPropertyValue("sessionAcknowledgeMode", acknowledgeMode);
			}
		}
		return builder.getBeanDefinition();
	}

	private Integer parseAcknowledgeMode(Element element) {
		String acknowledge = element.getAttribute(ACKNOWLEDGE_ATTRIBUTE);
		if (StringUtils.hasText(acknowledge)) {
			int acknowledgeMode = Session.AUTO_ACKNOWLEDGE;
			if (ACKNOWLEDGE_TRANSACTED.equals(acknowledge)) {
				acknowledgeMode = Session.SESSION_TRANSACTED;
			}
			else if (ACKNOWLEDGE_DUPS_OK.equals(acknowledge)) {
				acknowledgeMode = Session.DUPS_OK_ACKNOWLEDGE;
			}
			else if (ACKNOWLEDGE_CLIENT.equals(acknowledge)) {
				acknowledgeMode = Session.CLIENT_ACKNOWLEDGE;
			}
			else if (!ACKNOWLEDGE_AUTO.equals(acknowledge)) {
				throw new BeanCreationException("Invalid jms-source 'acknowledge' setting: " +
						"only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.");
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

}
