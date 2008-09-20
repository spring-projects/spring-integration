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

	public static final String HEADER_MAPPER_ATTRIBUTE = "header-mapper";

	public static final String HEADER_MAPPER_PROPERTY = "headerMapper";

	public static final String MESSAGE_CONVERTER_ATTRIBUTE = "message-converter";

	public static final String MESSAGE_CONVERTER_PROPERTY = "messageConverter";

	private static final String ACKNOWLEDGE_ATTRIBUTE = "acknowledge";

	private static final String ACKNOWLEDGE_AUTO = "auto";

	private static final String ACKNOWLEDGE_CLIENT = "client";

	private static final String ACKNOWLEDGE_DUPS_OK = "dups-ok";

	private static final String ACKNOWLEDGE_TRANSACTED = "transacted";


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

	public static Integer parseAcknowledgeMode(Element element) {
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
				throw new BeanCreationException("Invalid JMS 'acknowledge' setting: " +
						"only \"auto\", \"client\", \"dups-ok\" and \"transacted\" supported.");
			}
			return acknowledgeMode;
		}
		else {
			return null;
		}
	}

}
