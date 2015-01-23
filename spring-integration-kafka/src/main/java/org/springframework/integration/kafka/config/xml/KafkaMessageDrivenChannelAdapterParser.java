/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan.
 */
public class KafkaMessageDrivenChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageDrivenChannelAdapter.class);

		String container = element.getAttribute("listener-container");
		String connectionFactory = element.getAttribute("connection-factory");
		String topics = element.getAttribute("topics");
		String offsetManager = element.getAttribute("offset-manager");
		String errorHandler = element.getAttribute("error-handler");
		String taskExecutor = element.getAttribute("task-executor");
		String concurrency = element.getAttribute("concurrency");
		String maxFetch = element.getAttribute("max-fetch");
		String queueSize = element.getAttribute("queue-size");

		if (StringUtils.hasText(container) &&
				(StringUtils.hasText(connectionFactory) || StringUtils.hasText(topics)
						|| StringUtils.hasText(offsetManager) || StringUtils.hasText(errorHandler)
						|| StringUtils.hasText(taskExecutor) || StringUtils.hasText(concurrency)
						|| StringUtils.hasText(maxFetch) || StringUtils.hasText(queueSize))) {
			parserContext.getReaderContext().error("The 'listener-container' is mutually exclusive with " +
					"'connection-factory', 'topics', 'offset-manager', 'error-handler', 'task-executor', " +
					"'concurrency', 'max-fetch' and 'queue-size'.", element);
		}

		if (StringUtils.hasText(container)) {
			builder.addConstructorArgReference(container);
		}
		else {
			if (!StringUtils.hasText(connectionFactory)) {
				parserContext.getReaderContext().error("The 'connection-factory' attribute is required when " +
						"'listener-container' isn't provided.", element);
			}
			if (!StringUtils.hasText(topics)) {
				parserContext.getReaderContext().error("The 'topics' attribute is required when " +
						"'listener-container' isn't provided.", element);
			}

			BeanDefinitionBuilder containerBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageListenerContainer.class);

			containerBuilder.addConstructorArgReference(connectionFactory)
					.addConstructorArgValue(topics);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(containerBuilder, element, "offset-manager");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(containerBuilder, element, "error-handler");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(
					containerBuilder, element, "task-executor", "fetchTaskExecutor");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(containerBuilder, element, "concurrency");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(containerBuilder, element, "max-fetch");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(containerBuilder, element, "queue-size");

			builder.addConstructorArgValue(containerBuilder.getBeanDefinition());
		}

		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "key-decoder");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "payload-decoder");

		return builder.getBeanDefinition();
	}

}
