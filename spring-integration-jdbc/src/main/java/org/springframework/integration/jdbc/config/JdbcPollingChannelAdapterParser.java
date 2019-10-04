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
 * @author Gary Russell
 *
 * @since 2.0
 */
public class JdbcPollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
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
		String querySupplier = element.getAttribute("query-supplier");
		if (StringUtils.hasText(element.getAttribute("query"))
				&& StringUtils.hasText(element.getAttribute("query-supplier"))) {
			parserContext.getReaderContext().error("Only one of 'query' or 'query-supplier' is allowed", source);

		}
		if (!StringUtils.hasText(query) && !StringUtils.hasText(querySupplier)) {
			parserContext.getReaderContext()
					.error("The 'query' or 'query-supplier' attributes are required", element);
		}
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}
		if (StringUtils.hasText(query)) {
			builder.addConstructorArgValue(query);
		}
		else {
			builder.addConstructorArgReference(querySupplier);
		}

		if (StringUtils.hasText(element.getAttribute("update"))
				&& StringUtils.hasText(element.getAttribute("update-supplier"))) {
			parserContext.getReaderContext().error("Only one of 'update' or 'update-supplier' is allowed", source);

		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "row-mapper");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "update-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "select-sql-parameter-source");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update", "updateSql");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "update-supplier", "updateSqlSupplier");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "update-per-row");

		return builder.getBeanDefinition();
	}

}
