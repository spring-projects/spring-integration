/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for 'xmpp:xmpp-connection' element
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XmppConnectionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return XmppConnectionFactoryBean.class;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String serviceName = element.getAttribute("service-name");
		String user = element.getAttribute("user");

		if (!StringUtils.hasText(serviceName) && !StringUtils.hasText(user)) {
			parserContext.getReaderContext().error("One of 'service-name' or 'user' attributes is required", element);
		}

		String[] attributes = {"user", "password", "resource", "host", "port", "service-name",
				IntegrationNamespaceUtils.AUTO_STARTUP, IntegrationNamespaceUtils.PHASE};

		for (String attribute : attributes) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-mode", true);
	}

}
