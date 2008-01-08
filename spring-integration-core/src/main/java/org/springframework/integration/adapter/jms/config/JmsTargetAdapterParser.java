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
import org.springframework.integration.adapter.jms.JmsTargetAdapter;
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

	private static final String DESTINATION_PROPERTY = "destination";

	private static final String CHANNEL_ATTRIBUTE = "channel";

	private static final String CHANNEL_PROPERTY = "channel";


	protected Class<?> getBeanClass(Element element) {
		return JmsTargetAdapter.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected void doParse(Element element, BeanDefinitionBuilder builder) {
		String jmsTemplate = element.getAttribute(JMS_TEMPLATE_ATTRIBUTE);
		String connectionFactory = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
		String destination = element.getAttribute(DESTINATION_ATTRIBUTE);
		if (StringUtils.hasText(jmsTemplate)) {
			if (StringUtils.hasText(connectionFactory) || StringUtils.hasText(destination)) {
				throw new BeanCreationException("when providing a 'jms-template' reference, neither " +
						"'connection-factory' or 'destination' should be provided.");
			}
			builder.addPropertyReference(JMS_TEMPLATE_PROPERTY, jmsTemplate);
		}
		else if (StringUtils.hasText(connectionFactory) && StringUtils.hasText(destination)) {
			builder.addPropertyReference(CONNECTION_FACTORY_PROPERTY, connectionFactory);
			builder.addPropertyReference(DESTINATION_PROPERTY, destination);
		}
		else {
			throw new BeanCreationException("either a 'jms-template' reference or both " +
			"'connection-factory' and 'destination' references must be provided.");
		}
		String channel = element.getAttribute(CHANNEL_ATTRIBUTE);
		builder.addPropertyReference(CHANNEL_PROPERTY, channel);
	}

}
