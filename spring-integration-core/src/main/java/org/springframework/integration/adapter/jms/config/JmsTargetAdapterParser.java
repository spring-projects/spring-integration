/*
 * Copyright 2002-2007 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.adapter.jms.JmsTargetAdapter;
import org.springframework.integration.bus.Subscription;
import org.springframework.integration.endpoint.DefaultMessageEndpoint;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;jms-target/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class JmsTargetAdapterParser extends AbstractSingleBeanDefinitionParser {

	private static final String JMS_TEMPLATE_ATTRIBUTE = "jms-template";

	private static final String JMS_TEMPLATE_PROPERTY = "jmsTemplate";

	private static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	private static final String CONNECTION_FACTORY_PROPERTY = "connectionFactory";

	private static final String DESTINATION_ATTRIBUTE = "destination";

	private static final String DESTINATION_NAME_ATTRIBUTE = "destination-name";

	private static final String DESTINATION_PROPERTY = "destination";

	private static final String DESTINATION_NAME_PROPERTY = "destinationName";

	private static final String CHANNEL_ATTRIBUTE = "channel";

	private static final String HANDLER_PROPERTY = "handler";

	private static final String SUBSCRIPTION_PROPERTY = "subscription";


	protected Class<?> getBeanClass(Element element) {
		return DefaultMessageEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String jmsTemplate = element.getAttribute(JMS_TEMPLATE_ATTRIBUTE);
		String connectionFactory = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
		String destination = element.getAttribute(DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(DESTINATION_NAME_ATTRIBUTE);
		RootBeanDefinition adapterDef = new RootBeanDefinition(JmsTargetAdapter.class);
		if (StringUtils.hasText(jmsTemplate)) {
			if (StringUtils.hasText(connectionFactory) || StringUtils.hasText(destination) || StringUtils.hasText(destinationName)) {
				throw new BeanCreationException("when providing a 'jms-template' reference, none of " +
						"'connection-factory', 'destination', or 'destination-name' should be provided.");
			}
			adapterDef.getPropertyValues().addPropertyValue(JMS_TEMPLATE_PROPERTY, new RuntimeBeanReference(jmsTemplate));
		}
		else if (StringUtils.hasText(connectionFactory) && (StringUtils.hasText(destination) ^ StringUtils.hasText(destinationName))) {
			adapterDef.getPropertyValues().addPropertyValue(CONNECTION_FACTORY_PROPERTY, new RuntimeBeanReference(connectionFactory));
			if (StringUtils.hasText(destination)) {
				adapterDef.getPropertyValues().addPropertyValue(DESTINATION_PROPERTY, new RuntimeBeanReference(destination));
			}
			else {
				adapterDef.getPropertyValues().addPropertyValue(DESTINATION_NAME_PROPERTY, destinationName);
			}
		}
		else {
			throw new BeanCreationException("either a 'jms-template' reference or both " +
			"'connection-factory' and 'destination' (or 'destination-name') references must be provided.");
		}
		String channel = element.getAttribute(CHANNEL_ATTRIBUTE);
		Subscription subscription = new Subscription(channel);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		builder.addPropertyReference(HANDLER_PROPERTY, adapterBeanName);
		builder.addPropertyValue(SUBSCRIPTION_PROPERTY, subscription);
	}

}
