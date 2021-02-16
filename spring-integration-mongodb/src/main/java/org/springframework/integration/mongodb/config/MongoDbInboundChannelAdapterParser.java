/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.mongodb.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;

/**
 * Parser for MongoDb store inbound adapters
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class MongoDbInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MongoDbMessageSource.class);

		// Will parse and validate 'mongodb-template', 'mongodb-factory',
		// 'collection-name', 'collection-name-expression' and 'mongo-converter'
		MongoParserUtils.processCommonAttributes(element, parserContext, builder);

		BeanDefinition queryExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("query", "query-expression",
						parserContext, element, true);

		builder.addConstructorArgValue(queryExpressionDef);

		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("update", "update-expression",
						parserContext, element, false);
		builder.addPropertyValue("updateExpression", expressionDef);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "entity-class");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");

		return builder.getBeanDefinition();
	}

}
