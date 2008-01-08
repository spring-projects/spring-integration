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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.integration.adapter.jms.JmsMessageDrivenSourceAdapter;
import org.springframework.integration.adapter.jms.JmsPollingSourceAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;jms-source/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class JmsSourceAdapterParser extends AbstractSingleBeanDefinitionParser {

	private static final String JMS_TEMPLATE_ATTRIBUTE = "jms-template";

	private static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	private static final String CONNECTION_FACTORY_PROPERTY = "connectionFactory";

	private static final String DESTINATION_ATTRIBUTE = "destination";

	private static final String DESTINATION_PROPERTY = "destination";

	private static final String DESTINATION_NAME_ATTRIBUTE = "destination-name";

	private static final String DESTINATION_NAME_PROPERTY = "destinationName";

	private static final String CHANNEL_ATTRIBUTE = "channel";

	private static final String CHANNEL_PROPERTY = "channel";

	private static final String POLL_PERIOD_ATTRIBUTE = "poll-period";

	private static final String POLL_PERIOD_PROPERTY = "period";


	protected Class<?> getBeanClass(Element element) {
		if (StringUtils.hasText(element.getAttribute(POLL_PERIOD_ATTRIBUTE))) {
			return JmsPollingSourceAdapter.class;
		}
		return JmsMessageDrivenSourceAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		if (builder.getBeanDefinition().getBeanClass().equals(JmsPollingSourceAdapter.class)) {
			parsePollingSourceAdapter(element, builder);
		}
		else {
			parseMessageDrivenSourceAdapter(element, builder);
		}
		String channel = element.getAttribute(CHANNEL_ATTRIBUTE);
		builder.addPropertyReference(CHANNEL_PROPERTY, channel);
	}

	private void parsePollingSourceAdapter(Element element, BeanDefinitionBuilder builder) { 
		String pollPeriod = element.getAttribute(POLL_PERIOD_ATTRIBUTE);
		if (!StringUtils.hasText(pollPeriod)) {
			throw new BeanCreationException("'" + POLL_PERIOD_ATTRIBUTE +
					"' is required for a " + JmsPollingSourceAdapter.class.getSimpleName());
		}
		builder.addPropertyValue(POLL_PERIOD_PROPERTY, pollPeriod);
		String jmsTemplate = element.getAttribute(JMS_TEMPLATE_ATTRIBUTE);
		String connectionFactory = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
		String destination = element.getAttribute(DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(DESTINATION_NAME_ATTRIBUTE);
		if (StringUtils.hasText(jmsTemplate)) {
			if (StringUtils.hasText(connectionFactory) || StringUtils.hasText(destination) || StringUtils.hasText(destinationName)) {
				throw new BeanCreationException("when providing '" + JMS_TEMPLATE_ATTRIBUTE +
						"', none of '" + CONNECTION_FACTORY_ATTRIBUTE + "', '" + DESTINATION_ATTRIBUTE + 
						"', or '" + DESTINATION_NAME_ATTRIBUTE + "' should be provided.");
			}
			builder.addConstructorArgReference(jmsTemplate);
		}
		else if (StringUtils.hasText(connectionFactory) && (StringUtils.hasText(destination) || StringUtils.hasText(destinationName))) {
			builder.addConstructorArgReference(connectionFactory);
			if (StringUtils.hasText(destination)) {
				builder.addConstructorArgReference(destination);
			}
			else if (StringUtils.hasText(destinationName)) {
				builder.addConstructorArg(destinationName);
			}
		}
		else {
			throw new BeanCreationException("either a '" + JMS_TEMPLATE_ATTRIBUTE + "' or both '" + 
					CONNECTION_FACTORY_ATTRIBUTE + "' and '" + DESTINATION_ATTRIBUTE + "' (or '" +
					DESTINATION_NAME_ATTRIBUTE + "') attributes must be provided for a " +
					JmsPollingSourceAdapter.class.getSimpleName());
		}
	}

	private void parseMessageDrivenSourceAdapter(Element element, BeanDefinitionBuilder builder) {
		String connectionFactory = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
		String destination = element.getAttribute(DESTINATION_ATTRIBUTE);
		String destinationName = element.getAttribute(DESTINATION_NAME_ATTRIBUTE);
		if (StringUtils.hasText(element.getAttribute(JMS_TEMPLATE_ATTRIBUTE))) {
			throw new BeanCreationException(JmsMessageDrivenSourceAdapter.class.getSimpleName() +
					" does not accept a '" + JMS_TEMPLATE_ATTRIBUTE + "' reference, both " +
					"'" + CONNECTION_FACTORY_ATTRIBUTE + "' and '" + DESTINATION_ATTRIBUTE +
					"' (or '" + DESTINATION_NAME_ATTRIBUTE + "') must be provided.");
		}
		if (StringUtils.hasText(connectionFactory) && (StringUtils.hasText(destination) || StringUtils.hasText(destinationName))) {
			builder.addPropertyReference(CONNECTION_FACTORY_PROPERTY, connectionFactory);
			if (StringUtils.hasText(destination)) {
				builder.addPropertyReference(DESTINATION_PROPERTY, destination);
			}
			else {
				builder.addPropertyValue(DESTINATION_NAME_PROPERTY, destinationName);
			}
		}
		else {
			throw new BeanCreationException("Both '" + CONNECTION_FACTORY_ATTRIBUTE + "' and '" +
					DESTINATION_ATTRIBUTE + "' (or '" + DESTINATION_NAME_ATTRIBUTE + "') must be provided.");
		}	
	}

}
