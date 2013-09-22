/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.jpa.config.xml;

import static org.springframework.integration.config.xml.IntegrationNamespaceUtils.ensureMutualExclusivity;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The Parser for the Retrieving Jpa Outbound Gateway.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 *
 * @since 2.2
 *
 */
public class RetrievingJpaOutboundGatewayParser extends AbstractJpaOutboundGatewayParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {

		final BeanDefinitionBuilder jpaOutboundGatewayBuilder = super.parseHandler(gatewayElement, parserContext);

		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getOutboundGatewayJpaExecutorBuilder(gatewayElement, parserContext);

		if(!ensureMutualExclusivity(gatewayElement, "first-result", "first-result-expression")) {
			throw new BeanDefinitionStoreException("Attributes first-result and first-result-expression" +
					" are mutually exclusive to each other");
		}

		Expression firstResultExpression = null;
		if(gatewayElement.hasAttribute("first-result")) {
			String firstResult = gatewayElement.getAttribute("first-result");
			if(StringUtils.hasText(firstResult)) {
				firstResultExpression = new LiteralExpression(firstResult);
			}
		}
		else if(gatewayElement.hasAttribute("first-result-expression")) {
			String firstResultExp = gatewayElement.getAttribute("first-result-expression");
			if(StringUtils.hasText(firstResultExp)) {
				firstResultExpression = new SpelExpressionParser().parseExpression(firstResultExp);
			}
		}
		if(firstResultExpression != null) {
			jpaOutboundGatewayBuilder.addPropertyValue("firstRecordExpression", firstResultExpression);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(jpaOutboundGatewayBuilder, gatewayElement, "evaluation-context");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "max-number-of-results");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "delete-after-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "delete-in-batch");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "expect-single-result");

		final BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		final String gatewayId = this.resolveId(gatewayElement, jpaOutboundGatewayBuilder.getRawBeanDefinition(), parserContext);
		final String jpaExecutorBeanName = gatewayId + ".jpaExecutor";

		parserContext.registerBeanComponent(new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		jpaOutboundGatewayBuilder.addConstructorArgReference(jpaExecutorBeanName);
		jpaOutboundGatewayBuilder.addPropertyValue("gatewayType", OutboundGatewayType.RETRIEVING);
		return jpaOutboundGatewayBuilder;
	}
}
