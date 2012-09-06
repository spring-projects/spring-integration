/*
 * Copyright 2007-2012 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */
package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisCollectionPopulatingMessageHandler;
import org.springframework.util.StringUtils;
/**
 * Parser for redis:store-outbound-channel-adapter element
 *
 * @author Oleg Zhurakousky
 * @since 2.2
 */
public class RedisCollectionsOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisCollectionPopulatingMessageHandler.class);

		String redisTemplateRef = element.getAttribute("redis-template");
		String connectionFactory = element.getAttribute("connection-factory");
		if (StringUtils.hasText(redisTemplateRef) && StringUtils.hasText(connectionFactory)){
			parserContext.getReaderContext().error("Only one of 'redis-template' or 'connection-factory'" +
					" is allowed", element);
		}

		if (StringUtils.hasText(redisTemplateRef)){
			builder.addConstructorArgReference(redisTemplateRef);
		}
		else {
			if (!StringUtils.hasText(connectionFactory)) {
				connectionFactory = "redisConnectionFactory";
			}
			builder.addConstructorArgReference(connectionFactory);
		}

		boolean atLeastOneRequired = false;
		RootBeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("key", "key-expression",
						parserContext, element, atLeastOneRequired);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload-elements");

		RedisParserUtils.setRedisSerializers(StringUtils.hasText(redisTemplateRef), element, parserContext, builder);

		if (expressionDef != null){
			builder.addConstructorArgValue(expressionDef);
		}

		String mapKeyExpression = element.getAttribute("map-key-expression");
		if (StringUtils.hasText(mapKeyExpression)) {
			RootBeanDefinition mapKeyExpressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			mapKeyExpressionDef.getConstructorArgumentValues().addGenericArgumentValue(mapKeyExpression);
			builder.addPropertyValue("mapKeyExpression", mapKeyExpressionDef);
		}

		return builder.getBeanDefinition();
	}
}