/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.mqtt.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.ConstructorArgumentValues.ValueHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.util.StringUtils;

/**
 * Contains various utility methods for parsing Mqtt Adapter
 * specific namespace elements as well as for the generation of the respective
 * {@link org.springframework.beans.factory.config.BeanDefinition}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
final class MqttParserUtils {

	private MqttParserUtils() {

	}

	static void parseCommon(Element element, BeanDefinitionBuilder builder, ParserContext parserContext) {
		ValueHolder holder;
		int n = 0;
		String url = element.getAttribute("url");
		if (StringUtils.hasText(url)) {
			builder.addConstructorArgValue(url);
			holder = builder.getRawBeanDefinition().getConstructorArgumentValues().getIndexedArgumentValues().get(n++);
			holder.setType("java.lang.String");
		}
		builder.addConstructorArgValue(element.getAttribute("client-id"));
		holder = builder.getRawBeanDefinition().getConstructorArgumentValues().getIndexedArgumentValues().get(n);
		holder.setType("java.lang.String");
		String clientFactory = element.getAttribute("client-factory");
		if (StringUtils.hasText(clientFactory)) {
			builder.addConstructorArgReference(clientFactory);
		}
		else {
			if (!StringUtils.hasText(url)) {
				parserContext.getReaderContext().error("If no 'url' attribute is provided, a 'client-factory' " +
						"(with serverURIs) is required", element);
			}
			builder.addConstructorArgValue(new RootBeanDefinition(DefaultMqttPahoClientFactory.class));
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
	}

}
