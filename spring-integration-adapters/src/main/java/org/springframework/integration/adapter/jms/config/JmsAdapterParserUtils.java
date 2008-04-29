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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.util.StringUtils;

/**
 * Utility methods and constants for JMS adapter parsers.
 * 
 * @author Mark Fisher
 */
public abstract class JmsAdapterParserUtils {

	public static final String JMS_TEMPLATE_ATTRIBUTE = "jms-template";

	public static final String JMS_TEMPLATE_PROPERTY = "jmsTemplate";

	public static final String CONNECTION_FACTORY_ATTRIBUTE = "connection-factory";

	public static final String CONNECTION_FACTORY_PROPERTY = "connectionFactory";

	public static final String DESTINATION_ATTRIBUTE = "destination";

	public static final String DESTINATION_PROPERTY = "destination";

	public static final String DESTINATION_NAME_ATTRIBUTE = "destination-name";

	public static final String DESTINATION_NAME_PROPERTY = "destinationName";


	public static String determineConnectionFactoryBeanName(Element element) {
		String connectionFactoryBeanName = "connectionFactory";
		if (element.hasAttribute(CONNECTION_FACTORY_ATTRIBUTE)) {
			connectionFactoryBeanName = element.getAttribute(CONNECTION_FACTORY_ATTRIBUTE);
			if (!StringUtils.hasText(connectionFactoryBeanName)) {
				throw new BeanCreationException(
						"JMS adapter 'connection-factory' attribute must not be empty");
			}
		}
		return connectionFactoryBeanName;
	}

}
