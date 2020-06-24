/*
 * Copyright 2019 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the inbound channel adapter.
 *
 * @author Gary Russell
 * @author Anshul Mehra
 *
 * @since 5.4
 *
 */
public class KafkaInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageSource.class);
		builder.addConstructorArgReference(element.getAttribute("consumer-factory"));
		boolean hasConsumerProperties = StringUtils.hasText(element.getAttribute("consumer-properties"));
		if (hasConsumerProperties) {
			builder.addConstructorArgReference(element.getAttribute("consumer-properties"));
		}
		String attribute = element.getAttribute("ack-factory");
		if (StringUtils.hasText(attribute)) {
			builder.addConstructorArgReference(attribute);
		}
		attribute = element.getAttribute("allow-multi-fetch");
		if (StringUtils.hasText(attribute)) {
			builder.addConstructorArgValue(attribute);
		}
		if (!hasConsumerProperties) {
			builder.addConstructorArgValue(element.getAttribute("topics"));
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "client-id");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "group-id");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "rebalance-listener");
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "raw-header", "rawMessageHeader");
		return builder.getBeanDefinition();
	}

}
