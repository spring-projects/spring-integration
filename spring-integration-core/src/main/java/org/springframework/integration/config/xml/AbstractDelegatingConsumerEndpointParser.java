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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.expression.DynamicExpression;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser class for endpoints that delegate to a method invoker or
 * expression evaluator when handling consumed Messages. These classes
 * use a FactoryBean implementation to construct the actual endpoint
 * instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
abstract class AbstractDelegatingConsumerEndpointParser extends AbstractConsumerEndpointParser {

	@Override
	protected final BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getFactoryBeanClassName());
		BeanComponentDefinition innerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
		boolean hasRef = StringUtils.hasText(ref);
		boolean hasExpression = StringUtils.hasText(expression);
		Element scriptElement = DomUtils.getChildElementByTagName(element, "script");
		Element expressionElement = DomUtils.getChildElementByTagName(element, "expression");
		if (innerDefinition != null) {
			if (hasRef || hasExpression || expressionElement != null) {
				parserContext.getReaderContext().error(
						"Neither 'ref' nor 'expression' are permitted when an inner bean (<bean/>) is configured on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
				return null;
			}
			builder.addPropertyValue("targetObject", innerDefinition);
		}
		else if (scriptElement != null) {
			if (hasRef || hasExpression || expressionElement != null) {
				parserContext.getReaderContext().error(
						"Neither 'ref' nor 'expression' are permitted when an inner script element is configured on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
				return null;
			}
			BeanDefinition scriptBeanDefinition = parserContext.getDelegate().parseCustomElement(scriptElement, builder.getBeanDefinition());
			builder.addPropertyValue("targetObject", scriptBeanDefinition);
		}
		else if (expressionElement != null) {
			if (hasRef || hasExpression) {
				parserContext.getReaderContext().error(
						"Neither 'ref' nor 'expression' are permitted when an inner 'expression' element is configured on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
				return null;
			}
			BeanDefinitionBuilder dynamicExpressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					DynamicExpression.class);
			String key = expressionElement.getAttribute("key");
			String expressionSourceReference = expressionElement.getAttribute("source");
			dynamicExpressionBuilder.addConstructorArgValue(key);
			dynamicExpressionBuilder.addConstructorArgReference(expressionSourceReference);
			builder.addPropertyValue("expression", dynamicExpressionBuilder.getBeanDefinition());
		}
		else if (hasRef && hasExpression) {
			parserContext.getReaderContext().error(
					"Only one of 'ref' or 'expression' is permitted, not both, on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
			return null;
		}
		else if (hasRef) {
			builder.addPropertyReference("targetObject", ref);
		}
		else if (hasExpression) {
			builder.addPropertyValue("expressionString", expression);
		}
		else if (!this.hasDefaultOption()) {
			parserContext.getReaderContext().error("Exactly one of the 'ref' attribute, 'expression' attribute, " +
					"or inner bean (<bean/>) definition is required for element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
			return null;
		}
		String method = element.getAttribute(METHOD_ATTRIBUTE);
		if (StringUtils.hasText(method)) {
			if (hasExpression || expressionElement != null) {
				parserContext.getReaderContext().error(
						"A 'method' attribute is not permitted when configuring an 'expression' on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
			}
			if (hasRef || innerDefinition != null) {
				builder.addPropertyValue("targetMethodName", method);
			}
			else {
				parserContext.getReaderContext().error("A 'method' attribute is only permitted when either " +
						"a 'ref' or inner-bean definition is provided on element " +
					IntegrationNamespaceUtils.createElementDescription(element) + ".", source);
			}
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		this.postProcess(builder, element, parserContext);
		return builder;
	}

	/**
	 * Subclasses may override this no-op method to provide additional configuration.
	 *
	 * @param builder The builder.
	 * @param element The element.
	 * @param parserContext The parser context.
	 */
	void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
	}

	abstract boolean hasDefaultOption();

	abstract String getFactoryBeanClassName();

}
