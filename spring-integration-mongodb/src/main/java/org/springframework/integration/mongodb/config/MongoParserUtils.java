/*
 * Copyright 2002-2024 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Utility class used by mongo parsers.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
final class MongoParserUtils {

	private MongoParserUtils() {
	}

	/**
	 * Will parse and validate
	 * 'mongodb-template', 'mongodb-factory', 'collection-name', 'collection-name-expression' and 'mongo-converter'.
	 * @param element the element to parse
	 * @param parserContext the context for parsing
	 * @param builder the bean definition builder
	 */
	public static void processCommonAttributes(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		String mongoDbTemplate = element.getAttribute("mongo-template");
		String mongoDbFactory = element.getAttribute("mongodb-factory");

		if (StringUtils.hasText(mongoDbTemplate) && StringUtils.hasText(mongoDbFactory)) {
			parserContext.getReaderContext().error("Only one of '" + mongoDbTemplate + "' or '"
					+ mongoDbFactory + "' is allowed", element);
		}

		if (StringUtils.hasText(mongoDbTemplate)) {
			builder.addConstructorArgReference(mongoDbTemplate);
			if (StringUtils.hasText(element.getAttribute("mongo-converter"))) {
				parserContext.getReaderContext().error("'mongo-converter' is not allowed with 'mongo-template'",
						element);
			}
		}
		else {
			if (!StringUtils.hasText(mongoDbFactory)) {
				mongoDbFactory = "mongoDbFactory";
			}
			builder.addConstructorArgReference(mongoDbFactory);
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "mongo-converter");
		}

		BeanDefinition collectionNameExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("collection-name",
						"collection-name-expression", parserContext, element, false);

		if (collectionNameExpressionDef != null) {
			builder.addPropertyValue("collectionNameExpression", collectionNameExpressionDef);
		}
	}

}
