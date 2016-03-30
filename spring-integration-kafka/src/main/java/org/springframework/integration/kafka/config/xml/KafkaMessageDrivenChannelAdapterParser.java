/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.util.StringUtils;

/**
 *
 * Parser for the message driven channel adapter.
 *
 * @author Artem Bilan
 * @author Gary Russell
 */
public class KafkaMessageDrivenChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageDrivenChannelAdapter.class);

		String container = element.getAttribute("listener-container");
		if (StringUtils.hasText(container)) {
			builder.addConstructorArgReference(container);
		}
		else {
			parserContext.getReaderContext().error("The 'listener-container' attribute is required.", element);
		}

		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				"use-context-message-builder", "useMessageBuilderFactory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				"set-id", "generateMessageId");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				"set-timestamp", "generateTimestamp");

		return builder.getBeanDefinition();
	}

}
