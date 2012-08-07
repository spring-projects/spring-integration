/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisStoreInboundChannelAdapter;
import org.springframework.util.StringUtils;
/**
 * Parser for Redis store inbound adapters
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionsInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RedisStoreInboundChannelAdapter.class);
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		boolean atLeastOneRequired = true;
		RootBeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("key", "key-expression",
						parserContext, element, atLeastOneRequired);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");

		builder.addConstructorArgValue(expressionDef);

		String beanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(beanName);
	}
}
