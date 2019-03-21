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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.JdbcOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class JdbcOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		String dataSourceRef = element.getAttribute("data-source");
		String jdbcOperationsRef = element.getAttribute("jdbc-operations");
		boolean refToDataSourceSet = StringUtils.hasText(dataSourceRef);
		boolean refToJdbcOperationsSet = StringUtils.hasText(jdbcOperationsRef);

		if ((refToDataSourceSet && refToJdbcOperationsSet) || (!refToDataSourceSet && !refToJdbcOperationsSet)) {
			parserContext.getReaderContext().error(
					"Exactly one of the attributes data-source or "
							+ "simple-jdbc-operations should be set for the JDBC outbound-gateway", element);
		}

		String selectQuery = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "query",
				parserContext);
		String updateQuery = IntegrationNamespaceUtils.getTextFromAttributeOrNestedElement(element, "update",
				parserContext);

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(JdbcOutboundGateway.class);
		if (refToDataSourceSet) {
			builder.addConstructorArgReference(dataSourceRef);
		}
		else {
			builder.addConstructorArgReference(jdbcOperationsRef);
		}

		builder.addConstructorArgValue(updateQuery);
		builder.addConstructorArgValue(selectQuery);

		IntegrationNamespaceUtils
				.setReferenceIfAttributeDefined(builder, element, "reply-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"request-sql-parameter-source-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"request-prepared-statement-setter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "row-mapper");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "keys-generated");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");

		return builder;
	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

}
