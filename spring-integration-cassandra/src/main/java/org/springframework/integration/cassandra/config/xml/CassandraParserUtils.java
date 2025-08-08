/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config.xml;

import java.util.List;

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

	public static boolean areMutuallyExclusive(String query, BeanDefinition statementExpressionDef,
			String ingestQuery) {

		return !StringUtils.hasText(query) && statementExpressionDef == null && !StringUtils.hasText(ingestQuery)
				|| !(StringUtils.hasText(query) && statementExpressionDef != null && StringUtils.hasText(ingestQuery))
				&& (StringUtils.hasText(query) ^ statementExpressionDef != null) ^ StringUtils.hasText(ingestQuery);
	}

	private CassandraParserUtils() {
	}

}
