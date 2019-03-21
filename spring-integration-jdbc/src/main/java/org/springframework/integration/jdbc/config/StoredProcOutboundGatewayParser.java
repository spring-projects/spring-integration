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
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jdbc.StoredProcOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @since 2.1
 *
 */
public class StoredProcOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StoredProcOutboundGateway.class);

		final BeanDefinitionBuilder storedProcExecutorBuilder = StoredProcParserUtils.getStoredProcExecutorBuilder(element, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "is-function");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "return-value-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "use-payload-as-parameter-source");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(storedProcExecutorBuilder, element, "sql-parameter-source-factory");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(storedProcExecutorBuilder, element, "skip-undeclared-results");

		final ManagedMap<String, BeanMetadataElement> returningResultSetMap =
				StoredProcParserUtils.getReturningResultsetBeanDefinitions(element, parserContext);

		if (!returningResultSetMap.isEmpty()) {
			storedProcExecutorBuilder.addPropertyValue("returningResultSetRowMappers", returningResultSetMap);
		}

		final AbstractBeanDefinition storedProcExecutorBuilderBeanDefinition = storedProcExecutorBuilder.getBeanDefinition();
		final String gatewayId = this.resolveId(element, builder.getRawBeanDefinition(), parserContext);
		final String storedProcExecutorBeanName = gatewayId + ".storedProcExecutor";

		parserContext.registerBeanComponent(new BeanComponentDefinition(storedProcExecutorBuilderBeanDefinition, storedProcExecutorBeanName));

		builder.addConstructorArgReference(storedProcExecutorBeanName);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");

		String replyChannel = element.getAttribute("reply-channel");
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
