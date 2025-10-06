/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.outbound.JdbcMessageHandler;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class JdbcMessageHandlerParser extends AbstractOutboundChannelAdapterParser {

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
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-as-parameter-source",
				"usePayloadAsParameterSource");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "prepared-statement-setter");
		builder.addConstructorArgValue(query);
		return builder.getBeanDefinition();
	}

}
