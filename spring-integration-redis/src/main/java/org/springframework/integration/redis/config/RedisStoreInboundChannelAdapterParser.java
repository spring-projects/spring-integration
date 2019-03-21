/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;
import org.springframework.util.StringUtils;

/**
 * Parser for Redis store inbound adapters
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.2
 */
public class RedisStoreInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RedisStoreMessageSource.class);
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
		boolean atLeastOneRequired = true;
		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("key", "key-expression",
						parserContext, element, atLeastOneRequired);
		builder.addConstructorArgValue(expressionDef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");

		return builder.getBeanDefinition();
	}

}
