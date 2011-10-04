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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.StoredProcOutboundGateway;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Gunnar Hillert
 * @since 2.1
 * 
 */
public class StoredProcOutboundGatewayParser extends AbstractConsumerEndpointParser {
	
	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(StoredProcOutboundGateway.class);

		String dataSourceRef       = gatewayElement.getAttribute("data-source");
		String storedProcedureName = gatewayElement.getAttribute("stored-procedure-name");
		
		builder.addConstructorArgReference(dataSourceRef);
		builder.addConstructorArgValue(storedProcedureName);
		
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, gatewayElement, "is-function");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, gatewayElement, "ignore-column-meta-data");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, gatewayElement, "expect-single-result");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, gatewayElement, "return-value-required");
	
		final ManagedList<BeanDefinition> procedureParameterList       = StoredProcParserUtils.getProcedureParameterBeanDefinitions(gatewayElement, parserContext);
		final ManagedList<BeanDefinition> sqlParameterDefinitionList   = StoredProcParserUtils.getSqlParameterDefinitionBeanDefinitions(gatewayElement, parserContext);
		final ManagedMap<String, BeanDefinition> returningResultsetMap = StoredProcParserUtils.getReturningResultsetBeanDefinitions(gatewayElement, parserContext);

		if (!procedureParameterList.isEmpty()) {
			builder.addPropertyValue("procedureParameters", procedureParameterList);
		}
		if (!sqlParameterDefinitionList.isEmpty()) {
			builder.addPropertyValue("sqlParameters", sqlParameterDefinitionList);
		}
		if (!returningResultsetMap.isEmpty()) {
			builder.addPropertyValue("returningResultSetRowMappers", returningResultsetMap);
		}

		String replyChannel = gatewayElement.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("outputChannel", replyChannel);
		}

		return builder;
	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}
	
}
