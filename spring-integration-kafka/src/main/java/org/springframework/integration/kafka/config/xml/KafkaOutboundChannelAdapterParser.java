/*
 * Copyright 2013-2016 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;

/**
 * Parser for the outbound channel adapter.
 *
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @since 0.5
 *
 */
public class KafkaOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(final Element element, final ParserContext parserContext) {
		final BeanDefinitionBuilder kafkaProducerMessageHandlerBuilder =
								BeanDefinitionBuilder.genericBeanDefinition(KafkaProducerMessageHandler.class);

		final String kafkaTemplateBeanName = element.getAttribute("kafka-template");

		kafkaProducerMessageHandlerBuilder.addConstructorArgReference(kafkaTemplateBeanName);

		BeanDefinition topicExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("topic", "topic-expression",
						parserContext, element, false);
		if (topicExpressionDef != null) {
			kafkaProducerMessageHandlerBuilder.addPropertyValue("topicExpression", topicExpressionDef);
		}

		BeanDefinition messageKeyExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("message-key",
						"message-key-expression", parserContext, element, false);
		if (messageKeyExpressionDef != null) {
			kafkaProducerMessageHandlerBuilder.addPropertyValue("messageKeyExpression", messageKeyExpressionDef);
		}

		BeanDefinition partitionIdExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("partition-id",
						"partition-id-expression", parserContext, element, false);
		if (partitionIdExpressionDef != null) {
			kafkaProducerMessageHandlerBuilder.addPropertyValue("partitionIdExpression", partitionIdExpressionDef);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(kafkaProducerMessageHandlerBuilder, element, "sync");
		return kafkaProducerMessageHandlerBuilder.getBeanDefinition();
	}

}
