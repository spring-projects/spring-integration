/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.store.JdbcMessageStore;
import org.springframework.util.StringUtils;

/**
 * Parser for {@link JdbcMessageStore}.
 *
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class JdbcMessageStoreParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JdbcMessageStore.class);

		String dataSourceRef = element.getAttribute("data-source");
		String simpleJdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToSimpleJdbcOperationsSet = StringUtils.hasText(simpleJdbcOperationsRef);
		if ((refToDataSourceSet && refToSimpleJdbcOperationsSet)
				|| (!refToDataSourceSet && !refToSimpleJdbcOperationsSet)) {
			parserContext.getReaderContext().error(
					"Exactly one of the attributes data-source or "
							+ "simple-jdbc-operations should be set for the JDBC message-store", source);
		}

		builder.addConstructorArgReference(refToDataSourceSet ? dataSourceRef : simpleJdbcOperationsRef);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "deserializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "table-prefix");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "region");

		return builder.getBeanDefinition();

	}

}
