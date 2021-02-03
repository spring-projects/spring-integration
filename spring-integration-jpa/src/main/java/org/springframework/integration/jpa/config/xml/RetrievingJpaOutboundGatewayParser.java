/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jpa.config.xml;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * The Parser for the Retrieving Jpa Outbound Gateway.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class RetrievingJpaOutboundGatewayParser extends AbstractJpaOutboundGatewayParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {

		BeanDefinitionBuilder jpaOutboundGatewayBuilder = super.parseHandler(gatewayElement, parserContext);

		BeanDefinitionBuilder jpaExecutorBuilder =
				JpaParserUtils.getOutboundGatewayJpaExecutorBuilder(gatewayElement, parserContext);

		BeanDefinition firstResultExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("first-result", "first-result-expression",
						parserContext, gatewayElement, false);
		if (firstResultExpression != null) {
			jpaExecutorBuilder.addPropertyValue("firstResultExpression", firstResultExpression);
		}

		BeanDefinition maxResultsExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("max-results", "max-results-expression",
						parserContext, gatewayElement, false);
		if (maxResultsExpression != null) {
			jpaExecutorBuilder.addPropertyValue("maxResultsExpression", maxResultsExpression);
		}

		parseIdExpression(gatewayElement, parserContext, jpaExecutorBuilder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "delete-after-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(
				jpaExecutorBuilder, gatewayElement, "flush-after-delete", "flush");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "delete-in-batch");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "expect-single-result");

		BeanDefinition jpaExecutorBuilderBeanDefinition = jpaExecutorBuilder.getBeanDefinition();
		String gatewayId = resolveId(gatewayElement, jpaOutboundGatewayBuilder.getRawBeanDefinition(), parserContext);
		String jpaExecutorBeanName = gatewayId + ".jpaExecutor";

		parserContext.registerBeanComponent(
				new BeanComponentDefinition(jpaExecutorBuilderBeanDefinition, jpaExecutorBeanName));

		return jpaOutboundGatewayBuilder.addPropertyReference("jpaExecutor", jpaExecutorBeanName)
				.addPropertyValue("gatewayType", OutboundGatewayType.RETRIEVING);
	}

	private static void parseIdExpression(Element gatewayElement, ParserContext parserContext,
			BeanDefinitionBuilder jpaExecutorBuilder) {

		if (StringUtils.hasText(gatewayElement.getAttribute("id-expression"))) {
			String[] otherAttributes = { "jpa-query", "native-query", "named-query", "first-result",
					"first-result-expression", "max-results", "max-results-expression", "delete-in-batch",
					"expect-single-result", "parameter-source-factory", "use-payload-as-parameter-source" };

			String others =
					Arrays.stream(otherAttributes)
							.filter((attr) -> gatewayElement.hasAttribute(attr) &&
									StringUtils.hasText(gatewayElement.getAttribute(attr)))
							.collect(Collectors.joining(","));

			boolean childElementsExist = !CollectionUtils.isEmpty(DomUtils.getChildElementsByTagName(gatewayElement,
					"parameter"));
			if (others.length() > 0 || childElementsExist) {
				parserContext.getReaderContext().error(
						(others.length() == 0 ? "" : "'" + others + "' "
								+ (childElementsExist ? "and " : ""))
								+ (childElementsExist ? "child elements " : "")
								+ "not allowed with an 'id-expression' attribute.",
						gatewayElement);
			}
			BeanDefinition idExpressionDef =
					IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("id-expression", gatewayElement);
			jpaExecutorBuilder.addPropertyValue("idExpression", idExpressionDef);
		}
	}

}
