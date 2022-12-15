/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.webflux.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.config.HttpOutboundChannelAdapterParser;
import org.springframework.integration.webflux.outbound.WebFluxRequestExecutingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-channel-adapter' element of the webflux namespace.
 *
 * @author Artem Bilan
 * @author Jatin Saxena
 *
 * @since 5.0
 */
public class WebFluxOutboundChannelAdapterParser extends HttpOutboundChannelAdapterParser {

	@Override
	protected BeanDefinitionBuilder getBuilder(Element element, ParserContext parserContext) {
		return buildWebFluxRequestExecutingMessageHandler(element, parserContext);
	}

	static BeanDefinitionBuilder buildWebFluxRequestExecutingMessageHandler(Element element,
			ParserContext parserContext) {

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(WebFluxRequestExecutingMessageHandler.class);

		String webClientRef = element.getAttribute("web-client");
		if (StringUtils.hasText(webClientRef)) {
			if (element.hasAttribute("encoding-mode")) {
				parserContext.getReaderContext()
						.error("The 'web-client' and 'encoding-mode' attributes are mutually exclusive.", element);
			}

			builder.getBeanDefinition()
					.getConstructorArgumentValues()
					.addIndexedArgumentValue(1, new RuntimeBeanReference(webClientRef));
		}
		else {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "encoding-mode");
		}

		String type = element.getAttribute("publisher-element-type");
		String typeExpression = element.getAttribute("publisher-element-type-expression");

		boolean hasType = StringUtils.hasText(type);
		boolean hasTypeExpression = StringUtils.hasText(typeExpression);

		if (hasType && hasTypeExpression) {
			parserContext.getReaderContext()
					.error("The 'publisher-element-type' and 'publisher-element-type-expression' attributes " +
							"are mutually exclusive.", element);
		}

		if (hasType) {
			builder.addPropertyValue("publisherElementType", type);
		}
		else if (hasTypeExpression) {
			builder.addPropertyValue("publisherElementTypeExpression",
					BeanDefinitionBuilder.rootBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(typeExpression)
							.getBeanDefinition());
		}

		BeanDefinition attributeVariablesExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("attribute-variables-expression",
						element);

		if (attributeVariablesExpressionDef != null) {
			builder.addPropertyValue("attributeVariablesExpression", attributeVariablesExpressionDef);
		}

		return builder;
	}

}
