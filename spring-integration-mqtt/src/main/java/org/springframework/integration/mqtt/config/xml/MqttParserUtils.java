/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.mqtt.config.xml;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Contains various utility methods for parsing Mqtt Adapter
 * specific namesspace elements as well as for the generation of the the
 * respective {@link BeanDefinition}s.
 *
 * @author Gary Russell
 * @since 1.0
 *
 */
public final class MqttParserUtils {

	/** Prevent instantiation. */
	private MqttParserUtils() {
		throw new AssertionError();
	}

	public static void parseCommon(Element element, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(element.getAttribute("url"));
		builder.addConstructorArgValue(element.getAttribute("client-id"));
		String clientFactory = element.getAttribute("client-factory");
		if (StringUtils.hasText(clientFactory)) {
			builder.addConstructorArgReference(clientFactory);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
	}

}
