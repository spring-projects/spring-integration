/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.ExpressionArgumentsStrategy;
import org.springframework.integration.redis.outbound.RedisOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the {@code <int-redis:outbound-gateway/>} component.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class RedisOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisOutboundGateway.class);

		String redisTemplate = element.getAttribute("redis-template");
		String connectionFactory = element.getAttribute("connection-factory");
		if (StringUtils.hasText(redisTemplate) && StringUtils.hasText(connectionFactory)) {
			parserContext.getReaderContext().error("Only one of '" + redisTemplate + "' or '"
					+ connectionFactory + "' is allowed.", element);
		}
		if (StringUtils.hasText(redisTemplate)) {
			builder.addConstructorArgReference(redisTemplate);
		}
		else {
			if (!StringUtils.hasText(connectionFactory)) {
				connectionFactory = "redisConnectionFactory";
			}
			builder.addConstructorArgReference(connectionFactory);
		}

		String argumentExpressions = element.getAttribute("argument-expressions");
		boolean hasArgumentExpressions = StringUtils.hasText(argumentExpressions);
		String argumentsStrategy = element.getAttribute("arguments-strategy");
		boolean hasArgumentStrategy = element.hasAttribute("arguments-strategy");

		if (hasArgumentExpressions & hasArgumentStrategy) {
			parserContext.getReaderContext()
					.error("'argument-expressions' and 'arguments-strategy' are mutually exclusive.", element);
		}

		if (hasArgumentExpressions) {
			BeanDefinitionBuilder argumentsBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionArgumentsStrategy.class)
					.addConstructorArgValue(argumentExpressions)
					.addConstructorArgValue(element.getAttribute("use-command-variable"));
			builder.addPropertyValue("argumentsStrategy", argumentsBuilder.getBeanDefinition());
		}
		else if (StringUtils.hasLength(argumentsStrategy)) {
			builder.addPropertyReference("argumentsStrategy", argumentsStrategy);
		}
		else if (hasArgumentStrategy) {
			builder.addPropertyValue("argumentsStrategy", null);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "command-expression");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "arguments-serializer");

		return builder;
	}
}
