/*
 * Copyright 2022-present the original author or authors.
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

package org.springframework.integration.cassandra.config.xml;

import java.util.List;

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * The {@code int-cassandra} namespace XML parser helper.
 *
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class CassandraParserUtils {

	public static void processOutboundTypeAttributes(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {

		String cassandraTemplate = element.getAttribute("cassandra-template");
		String mode = element.getAttribute("mode");
		String ingestQuery = element.getAttribute("ingest-query");
		String query = element.getAttribute("query");

		if (!StringUtils.hasText(cassandraTemplate)) {
			parserContext.getReaderContext().error("cassandra-template is required", element);
		}

		builder.addConstructorArgReference(cassandraTemplate);
		if (StringUtils.hasText(mode)) {
			builder.addConstructorArgValue(mode);
		}

		BeanDefinition statementExpressionDef = IntegrationNamespaceUtils
				.createExpressionDefIfAttributeDefined("statement-expression", element);

		if (statementExpressionDef != null) {
			builder.addPropertyValue("statementExpression", statementExpressionDef);
		}

		if (!areMutuallyExclusive(query, statementExpressionDef, ingestQuery)) {
			parserContext.getReaderContext()
					.error("'query', 'ingest-query', 'statement-expression' are mutually exclusive", element);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "write-options");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ingest-query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async");

		List<Element> parameterExpressions = DomUtils.getChildElementsByTagName(element, "parameter-expression");
		if (!CollectionUtils.isEmpty(parameterExpressions)) {
			ManagedMap<String, Object> parameterExpressionsMap = new ManagedMap<>();
			for (Element parameterExpressionElement : parameterExpressions) {
				String name = parameterExpressionElement.getAttribute(AbstractBeanDefinitionParser.NAME_ATTRIBUTE);
				BeanDefinition expression =
						IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined(
								IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE, parameterExpressionElement);
				if (expression != null) {
					parameterExpressionsMap.put(name, expression);
				}
			}
			builder.addPropertyValue("parameterExpressions", parameterExpressionsMap);
		}

	}

	public static boolean areMutuallyExclusive(@Nullable String query, @Nullable BeanDefinition statementExpressionDef,
			String ingestQuery) {

		return !StringUtils.hasText(query) && statementExpressionDef == null && !StringUtils.hasText(ingestQuery)
				|| !(StringUtils.hasText(query) && statementExpressionDef != null && StringUtils.hasText(ingestQuery))
				&& (StringUtils.hasText(query) ^ statementExpressionDef != null) ^ StringUtils.hasText(ingestQuery);
	}

	private CassandraParserUtils() {
	}

}
