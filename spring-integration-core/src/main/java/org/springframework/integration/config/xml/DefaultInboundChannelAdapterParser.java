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

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.endpoint.ExpressionEvaluatingMessageSource;
import org.springframework.integration.endpoint.MethodInvokingMessageSource;
import org.springframework.integration.expression.DynamicExpression;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public class DefaultInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override // NOSONAR complexity
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) { // NOSONAR
		Object source = parserContext.extractSource(element);
		BeanMetadataElement result = null;
		BeanComponentDefinition innerBeanDef =
				IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String sourceRef = element.getAttribute(IntegrationNamespaceUtils.REF_ATTRIBUTE);
		String methodName = element.getAttribute(IntegrationNamespaceUtils.METHOD_ATTRIBUTE);
		String expressionString = element.getAttribute(IntegrationNamespaceUtils.EXPRESSION_ATTRIBUTE);
		Element scriptElement = DomUtils.getChildElementByTagName(element, "script");
		Element expressionElement = DomUtils.getChildElementByTagName(element, "expression");

		boolean hasInnerDef = innerBeanDef != null;
		boolean hasRef = StringUtils.hasText(sourceRef);
		boolean hasExpression = StringUtils.hasText(expressionString);
		boolean hasScriptElement = scriptElement != null;
		boolean hasExpressionElement = expressionElement != null;
		boolean hasMethod = StringUtils.hasText(methodName);

		if (!hasInnerDef && !hasRef && !hasExpression && !hasScriptElement && !hasExpressionElement) { // NOSONAR
			parserContext.getReaderContext().error(
					"Exactly one of the 'ref', 'expression', inner bean, <script> or <expression> is required.", element);
		}
		if (hasInnerDef) {
			if (hasRef || hasExpression) {
				parserContext.getReaderContext().error(
						"Neither 'ref' nor 'expression' are permitted when an inner bean (<bean/>) is configured on "
								+ "element " + IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
				return null;
			}
			if (hasMethod) {
				result = parseMethodInvokingSource(innerBeanDef, methodName, element, parserContext);
			}
			else {
				result = innerBeanDef.getBeanDefinition();
			}
		}
		else if (hasScriptElement) {
			if (hasRef || hasMethod || hasExpression) {
				parserContext.getReaderContext().error(
						"Neither 'ref' and 'method' nor 'expression' are permitted when an inner script element is "
								+ "configured on element "
								+ IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
				return null;
			}
			BeanDefinition scriptBeanDefinition = parserContext.getDelegate().parseCustomElement(scriptElement);
			BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationContextUtils.BASE_PACKAGE + ".scripting.ScriptExecutingMessageSource");
			sourceBuilder.addConstructorArgValue(scriptBeanDefinition);
			parseHeaderExpressions(sourceBuilder, element, parserContext);
			result = sourceBuilder.getBeanDefinition();
		}
		else if (hasExpression || hasExpressionElement) {
			if (hasRef || hasMethod) {
				parserContext.getReaderContext().error(
						"The 'ref' and 'method' attributes can't be used with 'expression' attribute or inner "
								+ "<expression>.", element);
				return null;
			}
			if (hasExpression & hasExpressionElement) {
				parserContext.getReaderContext().error(
						"Exactly one of the 'expression' attribute or inner <expression> is required.", element);
				return null;
			}
			result = parseExpression(expressionString, expressionElement, element, parserContext);
		}
		else if (hasRef) {
			BeanMetadataElement sourceValue = new RuntimeBeanReference(sourceRef);
			if (hasMethod) {
				result = parseMethodInvokingSource(sourceValue, methodName, element, parserContext);
			}
			else {
				result = sourceValue;
			}
		}
		return result;
	}

	private BeanMetadataElement parseMethodInvokingSource(BeanMetadataElement targetObject, String methodName,
			Element element, ParserContext parserContext) {

		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(MethodInvokingMessageSource.class);
		sourceBuilder.addPropertyValue("object", targetObject);
		sourceBuilder.addPropertyValue("methodName", methodName);
		this.parseHeaderExpressions(sourceBuilder, element, parserContext);
		return sourceBuilder.getBeanDefinition();
	}

	private BeanMetadataElement parseExpression(String expressionString, Element expressionElement, Element element,
			ParserContext parserContext) {

		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(ExpressionEvaluatingMessageSource.class);

		BeanDefinition expressionDef;

		if (StringUtils.hasText(expressionString)) {
			expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expressionString);
		}
		else {
			BeanDefinitionBuilder dynamicExpressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					DynamicExpression.class);
			String key = expressionElement.getAttribute("key");
			String expressionSourceReference = expressionElement.getAttribute("source");
			dynamicExpressionBuilder.addConstructorArgValue(key);
			dynamicExpressionBuilder.addConstructorArgReference(expressionSourceReference);
			expressionDef = dynamicExpressionBuilder.getBeanDefinition();
		}

		sourceBuilder.addConstructorArgValue(expressionDef);
		sourceBuilder.addConstructorArgValue(null);

		this.parseHeaderExpressions(sourceBuilder, element, parserContext);
		return sourceBuilder.getBeanDefinition();
	}

	private void parseHeaderExpressions(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "header");
		if (!CollectionUtils.isEmpty(headerElements)) {
			ManagedMap<String, Object> headerExpressions = new ManagedMap<>();
			for (Element headerElement : headerElements) {
				String headerName = headerElement.getAttribute("name");
				BeanDefinition expressionDef = IntegrationNamespaceUtils
						.createExpressionDefinitionFromValueOrExpression("value",
								"expression", parserContext, headerElement, true);
				headerExpressions.put(headerName, expressionDef);
			}
			builder.addPropertyValue("headerExpressions", headerExpressions);
		}
	}

}
