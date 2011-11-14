/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.StoredProcPollingChannelAdapter;
import org.w3c.dom.Element;

/**
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class StoredProcPollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StoredProcPollingChannelAdapter.class);

		String dataSourceRef       = element.getAttribute("data-source");
		String storedProcedureName = element.getAttribute("stored-procedure-name");

		builder.addConstructorArgReference(dataSourceRef);
		builder.addConstructorArgValue(storedProcedureName);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-column-meta-data");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "return-value-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "function");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "skip-undeclared-results");

		final ManagedList<BeanDefinition> procedureParameterList       = StoredProcParserUtils.getProcedureParameterBeanDefinitions(element, parserContext);
		final ManagedList<BeanDefinition> sqlParameterDefinitionList   = StoredProcParserUtils.getSqlParameterDefinitionBeanDefinitions(element, parserContext);
		final ManagedMap<String, BeanDefinition> returningResultsetMap = StoredProcParserUtils.getReturningResultsetBeanDefinitions(element, parserContext);

		if (!procedureParameterList.isEmpty()) {
			builder.addPropertyValue("procedureParameters", procedureParameterList);
		}
		if (!sqlParameterDefinitionList.isEmpty()) {
			builder.addPropertyValue("sqlParameters", sqlParameterDefinitionList);
		}
		if (!returningResultsetMap.isEmpty()) {
			builder.addPropertyValue("returningResultSetRowMappers", returningResultsetMap);
		}

		return builder.getBeanDefinition();

	}

}
