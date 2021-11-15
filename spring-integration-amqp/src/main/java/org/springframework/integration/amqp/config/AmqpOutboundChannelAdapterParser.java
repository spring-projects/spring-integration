/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.support.DefaultAmqpHeaderMapper;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the AMQP 'outbound-channel-adapter' element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1
 */
public class AmqpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AmqpOutboundEndpoint.class);
		String amqpTemplateRef = element.getAttribute("amqp-template");
		if (!StringUtils.hasText(amqpTemplateRef)) {
			amqpTemplateRef = "amqpTemplate";
			if (StringUtils.hasText(element.getAttribute("return-channel"))
					|| StringUtils.hasText(element.getAttribute("confirm-correlation-expression"))) {
				parserContext.getReaderContext().error("A dedicated 'amqp-template' is required when" +
						" using publisher confirms and returns", element);
			}
		}
		builder.addConstructorArgReference(amqpTemplateRef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "exchange-name", true);
		BeanDefinition exchangeNameExpression =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("exchange-name-expression", element);
		if (exchangeNameExpression != null) {
			builder.addPropertyValue("exchangeNameExpression", exchangeNameExpression);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "routing-key", true);
		BeanDefinition routingKeyExpression =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("routing-key-expression", element);
		if (routingKeyExpression != null) {
			builder.addPropertyValue("routingKeyExpression", routingKeyExpression);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-delivery-mode");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "lazy-connect");

		BeanDefinitionBuilder mapperBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(DefaultAmqpHeaderMapper.class);
		mapperBuilder.setFactoryMethod("outboundMapper");
		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext,
				mapperBuilder, null);

		BeanDefinition confirmCorrelationExpression =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("confirm-correlation-expression", element);
		if (confirmCorrelationExpression != null) {
			builder.addPropertyValue("confirmCorrelationExpression", confirmCorrelationExpression);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "confirm-ack-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "confirm-nack-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "confirm-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "wait-for-confirm");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "multi-send");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "return-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-message-strategy");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delay-expression",
				"delayExpressionString");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "headers-last", "headersMappedLast");

		return builder.getBeanDefinition();
	}

}
