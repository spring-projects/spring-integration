/*
 * Copyright 2020 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.channel.PollableKafkaChannel;
import org.springframework.integration.kafka.channel.PublishSubscribeKafkaChannel;
import org.springframework.integration.kafka.channel.SubscribableKafkaChannel;
import org.springframework.util.StringUtils;

/**
 * Parser for a channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder;
		String factory = element.getAttribute("container-factory");
		boolean hasFactory = StringUtils.hasText(factory);
		String source = element.getAttribute("message-source");
		boolean hasSource = StringUtils.hasText(source);
		String template = element.getAttribute("kafka-template");
		String topic = element.getAttribute("topic");
		boolean pubSub = "publish-subscribe-channel".equals(element.getLocalName());
		if (hasFactory) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(pubSub
					? PublishSubscribeKafkaChannel.class
					: SubscribableKafkaChannel.class);
			builder.addConstructorArgReference(template);
			builder.addConstructorArgReference(factory);
			builder.addConstructorArgValue(topic);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "role");
		}
		else if (hasSource) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(PollableKafkaChannel.class);
			builder.addConstructorArgReference(template);
			builder.addConstructorArgReference(source);
		}
		else {
			if (pubSub) {
				parserContext.getReaderContext().error("A 'container-factory' is required", element);
			}
			else {
				parserContext.getReaderContext().error("Either a 'container-factory' or 'message-source' is required",
						element);
			}
			return null;
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "group-id");
		return builder;
	}

}
