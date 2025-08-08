/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.JdbcMessageHandler;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Artem Bilan
 * @since 2.0
 *
 */
public class JdbcMessageHandlerParser extends AbstractOutboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(JdbcMessageHandler.class);
		String dataSourceRef = element.getAttribute("data-source");
		String jdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToJdbcOperationsSet = StringUtils.hasText(jdbcOperationsRef);
		if ((refToDataSourceSet && refToJdbcOperationsSet)
				|| (!refToDataSourceSet && !refToJdbcOperationsSet)) {
			parserContext.getReaderContext().error(
					"Exactly one of the attributes data-source or "
							+ "simple-jdbc-operations should be set for the JDBC outbound-channel-adapter", source);
		}
		String query = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "query", parserContext);
		if (!StringUtils.hasText(query)) {
			throw new BeanCreationException("The query attribute is required");
		}
		if (!StringUtils.hasText(query)) {
			throw new BeanCreationException("The query attribute is required");
		}
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "prepared-statement-setter");
		builder.addConstructorArgValue(query);
		return builder.getBeanDefinition();
	}

}
