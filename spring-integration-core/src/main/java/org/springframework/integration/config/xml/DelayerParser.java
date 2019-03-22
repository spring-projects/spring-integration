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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.expression.DynamicExpression;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;delayer&gt; element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 1.0.3
 */
public class DelayerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DelayHandler.class);

		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!StringUtils.hasText(id)) {
			parserContext.getReaderContext().error("The 'id' attribute is required.", element);
		}

		String defaultDelay = element.getAttribute("default-delay");
		String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
		Element expressionElement = DomUtils.getChildElementByTagName(element, "expression");

		boolean hasDefaultDelay = StringUtils.hasText(defaultDelay);
		boolean hasExpression = StringUtils.hasText(expression);
		boolean hasExpressionElement = expressionElement != null;

		if (!(hasDefaultDelay | hasExpression | hasExpressionElement)) {
			parserContext.getReaderContext()
					.error("The 'default-delay' or 'expression' attributes, or 'expression' sub-element should be provided.", element);
		}

		if (hasExpression & hasExpressionElement) {
			parserContext.getReaderContext()
					.error("'expression' attribute and 'expression' sub-element are mutually exclusive.", element);
		}

		builder.addConstructorArgValue(id + ".messageGroupId");

		String scheduler = element.getAttribute("scheduler");
		if (StringUtils.hasText(scheduler)) {
			builder.addConstructorArgReference(scheduler);
		}

		if (hasDefaultDelay) {
			builder.addPropertyValue("defaultDelay", defaultDelay);
		}

		BeanDefinitionBuilder expressionBuilder = null;
		if (hasExpression) {
			expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(expression);
		}
		else if (expressionElement != null) {
			expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					DynamicExpression.class);
			String key = expressionElement.getAttribute("key");
			String expressionSourceReference = expressionElement.getAttribute("source");
			expressionBuilder.addConstructorArgValue(key);
			expressionBuilder.addConstructorArgReference(expressionSourceReference);
		}

		if (expressionBuilder != null) {
			builder.addPropertyValue("delayExpression", expressionBuilder.getBeanDefinition());
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-store");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-expression-failures");

		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		Element adviceChainElement = DomUtils.getChildElementByTagName(element, "advice-chain");

		IntegrationNamespaceUtils.configureAndSetAdviceChainIfPresent(adviceChainElement, txElement,
				builder.getRawBeanDefinition(), parserContext, "delayedAdviceChain");

		if (txElement != null) {
			element.removeChild(txElement);
		}

		return builder;
	}

}
