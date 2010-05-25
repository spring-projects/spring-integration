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

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class NotificationListeningChannelAdapterParser extends AbstractSimpleBeanDefinitionParser {
	
	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.jmx.NotificationListeningMessageProducer";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		Object source = parserContext.extractSource(element);
		String channel = element.getAttribute("channel");
		if (!StringUtils.hasText(channel)) {
			parserContext.getReaderContext().error("The 'channel' attribute is required.", source);
		}
		builder.addPropertyReference("outputChannel", channel);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "mbean-server", "server");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "notification-filter", "filter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "handback");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "object-name");
	}

}
