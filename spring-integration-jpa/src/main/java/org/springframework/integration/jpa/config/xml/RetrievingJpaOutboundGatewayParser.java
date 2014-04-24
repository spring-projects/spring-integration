/*
 * Copyright 2002-2013 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
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
 * @since 2.2
 */
public class RetrievingJpaOutboundGatewayParser extends AbstractJpaOutboundGatewayParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element gatewayElement, ParserContext parserContext) {

		final BeanDefinitionBuilder jpaOutboundGatewayBuilder = super.parseHandler(gatewayElement, parserContext);

		final BeanDefinitionBuilder jpaExecutorBuilder = JpaParserUtils.getOutboundGatewayJpaExecutorBuilder(gatewayElement, parserContext);

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

		String idExpression = gatewayElement.getAttribute("id-expression");
		if (StringUtils.hasText(idExpression)) {
			String[] otherAttributes = {"jpa-query", "native-query", "named-query", "first-result",
					"first-result-expression", "max-results", "max-results-expression", "delete-in-batch",
					"expect-single-result", "parameter-source-factory", "use-payload-as-parameter-source"};
			StringBuilder others = new StringBuilder();
			for (String otherAttribute : otherAttributes) {
				if (gatewayElement.hasAttribute(otherAttribute) &&
						StringUtils.hasText(gatewayElement.getAttribute(otherAttribute))) {
					if (others.length() > 0) {
						others.append(", ");
					}
					others.append(otherAttribute);
				}
			}
			boolean childElementsExist = !CollectionUtils.isEmpty(DomUtils.getChildElementsByTagName(gatewayElement,
					"parameter"));
			if (others.length() > 0 || childElementsExist) {
				parserContext.getReaderContext().error(
						(others.length() == 0 ? "" : "'" + others.toString() + "' "
									+ (childElementsExist ? "and " : ""))
								+ (childElementsExist ? "child elements " : "")
								+ "not allowed with an 'id-expression' attribute.",
						gatewayElement);
			}
			AbstractBeanDefinition idExpressionDef = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
					.addConstructorArgValue(idExpression)
					.getBeanDefinition();
			jpaExecutorBuilder.addPropertyValue("idExpression", idExpressionDef);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "delete-after-poll");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(jpaExecutorBuilder, gatewayElement, "flush-after-delete", "flush");
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
