/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.JdbcPollingChannelAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for {@link JdbcPollingChannelAdapter}.
 *
 * @author Jonas Partner
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JdbcPollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(JdbcPollingChannelAdapter.class);
		String dataSourceRef = element.getAttribute("data-source");
		String jdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToJdbcOperationsSet = StringUtils.hasText(jdbcOperationsRef);
		if ((refToDataSourceSet && refToJdbcOperationsSet)
				|| (!refToDataSourceSet && !refToJdbcOperationsSet)) {
			parserContext.getReaderContext().error("Exactly one of the attributes data-source or " +
					"simple-jdbc-operations should be set for the JDBC inbound-channel-adapter", source);
		}
		String query = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "query", parserContext);
		if (!StringUtils.hasText(query)) {
			parserContext.getReaderContext()
					.error("The 'query' attribute is required", element);
		}
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}
		builder.addConstructorArgValue(query);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "row-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "update-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "select-sql-parameter-source");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update", "updateSql");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update-per-row");

		return builder.getBeanDefinition();
	}

}
