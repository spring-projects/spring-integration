/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.integration.transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'enricher' element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Liujiong
 * @author Kris Jacyna
 * @author Gary Russell
 * @author Ngoc Nhan
 * @since 2.1
 */
public class EnricherParser extends AbstractConsumerEndpointParser {

	private static final String TYPE_ATTRIBUTE = "type";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ContentEnricher.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		propertySubElements(element, parserContext, builder);

		headerSubElements(element, parserContext, builder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "should-clone-payload");

		String requestPayloadExpression = element.getAttribute("request-payload-expression");

		if (StringUtils.hasText(requestPayloadExpression)) {
			BeanDefinitionBuilder expressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(requestPayloadExpression);
			builder.addPropertyValue("requestPayloadExpression", expressionBuilder.getBeanDefinition());
		}

		return builder;
	}

	private void propertySubElements(Element element, ParserContext parserContext,
			final BeanDefinitionBuilder builder) {
		List<Element> subElements = DomUtils.getChildElementsByTagName(element, "property");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			ManagedMap<String, Object> nullResultExpressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");

				String value = subElement.getAttribute("value");
				String type = subElement.getAttribute(TYPE_ATTRIBUTE);
				String expression = subElement.getAttribute(EXPRESSION_ATTRIBUTE);
				String nullResultExpression = subElement.getAttribute("null-result-expression");
				boolean hasAttributeValue = StringUtils.hasText(value);
				boolean hasAttributeExpression = StringUtils.hasText(expression);
				boolean hasAttributeNullResultExpression = StringUtils.hasText(nullResultExpression);

				if (hasAttributeValue && hasAttributeExpression) {
					parserContext.getReaderContext().error("Only one of 'value' or 'expression' is allowed", element);
				}

				if (!hasAttributeValue && !hasAttributeExpression && !hasAttributeNullResultExpression) {
					parserContext.getReaderContext()
							.error("One of 'value' or 'expression' or 'null-result-expression' is required", element);
				}

				expression(element, parserContext, expressions, nullResultExpressions, name, value, type, expression,
						nullResultExpression, hasAttributeValue, hasAttributeExpression,
						hasAttributeNullResultExpression);
			}
			if (!expressions.isEmpty()) {
				builder.addPropertyValue("propertyExpressions", expressions);
			}
			if (!nullResultExpressions.isEmpty()) {
				builder.addPropertyValue("nullResultPropertyExpressions", nullResultExpressions);
			}
		}
	}

	private void expression(Element element, ParserContext parserContext, ManagedMap<String, Object> expressions,
			ManagedMap<String, Object> nullResultExpressions, String name, String value, String type, String expression,
			String nullResultExpression, boolean hasAttributeValue, boolean hasAttributeExpression,
			boolean hasAttributeNullResultExpression) {

		BeanDefinition expressionDef = null;
		BeanDefinition nullResultExpressionExpressionDef;

		if (hasAttributeValue) {
			BeanDefinitionBuilder expressionBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ValueExpression.class);
			if (StringUtils.hasText(type)) {
				expressionBuilder.addConstructorArgValue(new TypedStringValue(value, type));
			}
			else {
				expressionBuilder.addConstructorArgValue(value);
			}
			expressionDef = expressionBuilder.getBeanDefinition();
		}
		else if (hasAttributeExpression) {
			if (StringUtils.hasText(type)) {
				parserContext.getReaderContext().error("The 'type' attribute for '<property>' of '<enricher>' " +
						"is not allowed with an 'expression' attribute.", element);
			}
			expressionDef = BeanDefinitionBuilder
					.genericBeanDefinition(ExpressionFactoryBean.class)
					.addConstructorArgValue(expression)
					.getBeanDefinition();
		}
		if (expressionDef != null) {
			expressions.put(name, expressionDef);
		}
		if (hasAttributeNullResultExpression) {
			nullResultExpressionExpressionDef =
					BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
							.addConstructorArgValue(nullResultExpression).getBeanDefinition();
			nullResultExpressions.put(name, nullResultExpressionExpressionDef);
		}
	}

	private void headerSubElements(Element element, ParserContext parserContext, final BeanDefinitionBuilder builder) {
		List<Element> subElements;
		subElements = DomUtils.getChildElementsByTagName(element, "header");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			ManagedMap<String, Object> nullResultHeaderExpressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");
				String nullResultHeaderExpression = subElement.getAttribute("null-result-expression");
				String valueElementValue = subElement.getAttribute("value");
				String expressionElementValue = subElement.getAttribute(EXPRESSION_ATTRIBUTE);
				boolean hasAttributeValue = StringUtils.hasText(valueElementValue);
				boolean hasAttributeExpression = StringUtils.hasText(expressionElementValue);
				boolean hasAttributeNullResultExpression = StringUtils.hasText(nullResultHeaderExpression);
				if (hasAttributeValue && hasAttributeExpression) {
					parserContext.getReaderContext().error("Only one of '" + "value" + "' or '"
							+ EXPRESSION_ATTRIBUTE + "' is allowed", subElement);
				}

				if (!hasAttributeValue && !hasAttributeExpression && !hasAttributeNullResultExpression) {
					parserContext.getReaderContext()
							.error("One of 'value' or 'expression' or 'null-result-expression' is required",
									subElement);
				}
				headerExpression(parserContext, expressions, nullResultHeaderExpressions, subElement, name,
						valueElementValue, hasAttributeValue, hasAttributeExpression, hasAttributeNullResultExpression);
			}
			if (!expressions.isEmpty()) {
				builder.addPropertyValue("headerExpressions", expressions);
			}
			if (!nullResultHeaderExpressions.isEmpty()) {
				builder.addPropertyValue("nullResultHeaderExpressions", nullResultHeaderExpressions);
			}
		}
	}

	private void headerExpression(ParserContext parserContext, ManagedMap<String, Object> expressions,
			ManagedMap<String, Object> nullResultHeaderExpressions, Element subElement, String name,
			String valueElementValue, boolean hasAttributeValue, boolean hasAttributeExpression,
			boolean hasAttributeNullResultExpression) {

		BeanDefinition expressionDef = null;
		if (hasAttributeValue) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(valueElementValue);
		}
		else if (hasAttributeExpression) {
			expressionDef =
					IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined(EXPRESSION_ATTRIBUTE,
							subElement);
		}

		if (StringUtils.hasText(subElement.getAttribute(EXPRESSION_ATTRIBUTE))
				&& StringUtils.hasText(subElement.getAttribute(TYPE_ATTRIBUTE))) {
			parserContext.getReaderContext()
					.warning("The use of a 'type' attribute is deprecated since 4.0 "
							+ "when using 'expression'", subElement);
		}
		if (expressionDef != null) {
			BeanDefinitionBuilder valueProcessorBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ExpressionEvaluatingHeaderValueMessageProcessor.class)
					.addConstructorArgValue(expressionDef)
					.addConstructorArgValue(subElement.getAttribute(TYPE_ATTRIBUTE));
			IntegrationNamespaceUtils.setValueIfAttributeDefined(valueProcessorBuilder, subElement,
					"overwrite");
			expressions.put(name, valueProcessorBuilder.getBeanDefinition());
		}
		if (hasAttributeNullResultExpression) {
			BeanDefinition nullResultExpressionDefinition = IntegrationNamespaceUtils
					.createExpressionDefIfAttributeDefined("null-result-expression", subElement);
			BeanDefinitionBuilder nullResultValueProcessorBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ExpressionEvaluatingHeaderValueMessageProcessor.class)
					.addConstructorArgValue(nullResultExpressionDefinition)
					.addConstructorArgValue(subElement.getAttribute(TYPE_ATTRIBUTE));
			IntegrationNamespaceUtils.setValueIfAttributeDefined(nullResultValueProcessorBuilder, subElement,
					"overwrite");
			nullResultHeaderExpressions.put(name, nullResultValueProcessorBuilder.getBeanDefinition());
		}
	}

	@Override
	protected boolean replyChannelInChainAllowed(Element element) {
		return true;
	}

}
