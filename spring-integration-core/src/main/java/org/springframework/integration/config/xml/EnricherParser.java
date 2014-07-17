/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.List;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the 'enricher' element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class EnricherParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ContentEnricher.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		List<Element> subElements = DomUtils.getChildElementsByTagName(element, "property");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			ManagedMap<String, Object> nullResultExpressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");

				String value = subElement.getAttribute("value");
				String type = subElement.getAttribute("type");
				String expression = subElement.getAttribute("expression");
				String nullResultExpression = subElement.getAttribute("null-result-expression");
				boolean hasAttributeValue = StringUtils.hasText(value);
				boolean hasAttributeExpression = StringUtils.hasText(expression);

				if (hasAttributeValue && hasAttributeExpression){
					parserContext.getReaderContext().error("Only one of 'value' or 'expression' is allowed", element);
				}

				if (!hasAttributeValue && !hasAttributeExpression){
					parserContext.getReaderContext().error("One of 'value' or 'expression' is required", element);
				}

				BeanDefinition expressionDef;
				BeanDefinition nullResultExpressionExpressionDef;

				if (hasAttributeValue) {
					BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ValueExpression.class);
					if (StringUtils.hasText(type)) {
						expressionBuilder.addConstructorArgValue(new TypedStringValue(value, type));
					}
					else {
						expressionBuilder.addConstructorArgValue(value);
					}
					expressionDef = expressionBuilder.getBeanDefinition();
				}
				else {
					if (StringUtils.hasText(type)) {
						parserContext.getReaderContext().error("The 'type' attribute for '<property>' of '<enricher>' " +
										"is not allowed with an 'expression' attribute.", element);
					}
					expressionDef = BeanDefinitionBuilder
							.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(expression)
							.getBeanDefinition();
				}

				if (StringUtils.hasText(nullResultExpression)) {
					nullResultExpressionExpressionDef = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(nullResultExpression).getBeanDefinition();
					nullResultExpressions.put(name, nullResultExpressionExpressionDef);
				}
				expressions.put(name, expressionDef);
			}
			builder.addPropertyValue("propertyExpressions", expressions);
			builder.addPropertyValue("nullResultPropertyExpressions", nullResultExpressions);
		}

		subElements = DomUtils.getChildElementsByTagName(element, "header");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			ManagedMap<String, Object> nullResultHeaderExpressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");
				String nullResultHeaderExpression = subElement.getAttribute("null-result-expression");
				BeanDefinition expressionDefinition = IntegrationNamespaceUtils
						.createExpressionDefinitionFromValueOrExpression("value", "expression", parserContext,
								subElement, true);
				if (StringUtils.hasText(subElement.getAttribute("expression"))
						&& StringUtils.hasText(subElement.getAttribute("type"))) {
					parserContext.getReaderContext()
							.warning("The use of a 'type' attribute is deprecated since 4.0 "
									+ "when using 'expression'", element);
				}
				BeanDefinitionBuilder valueProcessorBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(ExpressionEvaluatingHeaderValueMessageProcessor.class)
						.addConstructorArgValue(expressionDefinition)
						.addConstructorArgValue(subElement.getAttribute("type"));
				if (StringUtils.hasText(nullResultHeaderExpression)) {
					BeanDefinition nullResultExpressionExpressionDef = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
						.addConstructorArgValue(nullResultHeaderExpression).getBeanDefinition();
					nullResultHeaderExpressions.put(name, nullResultExpressionExpressionDef);
				}
				IntegrationNamespaceUtils.setValueIfAttributeDefined(valueProcessorBuilder, subElement, "overwrite");
				expressions.put(name, valueProcessorBuilder.getBeanDefinition());

			}
			builder.addPropertyValue("headerExpressions", expressions);
			builder.addPropertyValue("nullResultHeaderExpressions", nullResultHeaderExpressions);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "should-clone-payload");

		String requestPayloadExpression = element.getAttribute("request-payload-expression");

		if (StringUtils.hasText(requestPayloadExpression)) {
			BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(requestPayloadExpression);
			builder.addPropertyValue("requestPayloadExpression", expressionBuilder.getBeanDefinition());
		}

		return builder;
	}

}
