/*
 * Copyright 2019-2020 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Utilities to assist with parsing XML.
 *
 * @author Gary Russell
 * @author Tom van den Berge
 *
 * @since 5.4
 *
 */
public final class KafkaParsingUtils {

	private KafkaParsingUtils() {
		super();
	}

	public static void commonOutboundProperties(final Element element, final ParserContext parserContext,
			final BeanDefinitionBuilder builder) {

		final String kafkaTemplateBeanName = element.getAttribute("kafka-template");
		builder.addConstructorArgReference(kafkaTemplateBeanName);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-message-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "send-failure-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "send-success-channel");

		BeanDefinition topicExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("topic", "topic-expression",
						parserContext, element, false);
		if (topicExpressionDef != null) {
			builder.addPropertyValue("topicExpression", topicExpressionDef);
		}

		BeanDefinition messageKeyExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("message-key",
						"message-key-expression", parserContext, element, false);
		if (messageKeyExpressionDef != null) {
			builder.addPropertyValue("messageKeyExpression", messageKeyExpressionDef);
		}

		BeanDefinition partitionIdExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("partition-id",
						"partition-id-expression", parserContext, element, false);
		if (partitionIdExpressionDef != null) {
			builder.addPropertyValue("partitionIdExpression", partitionIdExpressionDef);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "sync");

		BeanDefinition sendTimeoutExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("send-timeout",
						"send-timeout-expression", parserContext, element, false);
		if (sendTimeoutExpressionDef != null) {
			builder.addPropertyValue("sendTimeoutExpression", sendTimeoutExpressionDef);
		}

		BeanDefinition timestampExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("timestamp-expression", element);

		if (timestampExpressionDef != null) {
			builder.addPropertyValue("timestampExpression", timestampExpressionDef);
		}

		BeanDefinition flushExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("flush-expression", element);

		if (flushExpressionDef != null) {
			builder.addPropertyValue("flushExpression", flushExpressionDef);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "header-mapper");
	}

}
