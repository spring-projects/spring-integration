/*
 * Copyright 2014 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.IdempotentReceiverAutoProxyCreatorIntegrationConfigurationInitializer;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.ExpressionMetadataKeyStrategy;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;idempotent-receiver/&gt; element.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class IdempotentReceiverInterceptorParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);

		String selector = element.getAttribute("selector");
		boolean hasSelector = StringUtils.hasText(selector);
		String store = element.getAttribute("metadata-store");
		boolean hasStore = StringUtils.hasText(store);
		String strategy = element.getAttribute("key-strategy");
		boolean hasStrategy = StringUtils.hasText(strategy);
		String expression = element.getAttribute("key-expression");
		boolean hasExpression = StringUtils.hasText(expression);

		String endpoints = element.getAttribute("endpoint");

		if (!hasSelector & !(hasStrategy | hasExpression)) {
			parserContext.getReaderContext().error("One of the 'selector', 'key-strategy' or 'key-expression' " +
					"attributes must be provided", source);
		}

		if (hasSelector & (hasStore | hasStrategy | hasExpression)) {
			parserContext.getReaderContext().error("The 'selector' attribute is mutually exclusive with " +
					"'metadata-store', 'key-strategy' or 'key-expression'", source);
		}

		if (hasStrategy & hasExpression) {
			parserContext.getReaderContext().error("The 'key-strategy' and 'key-expression' attributes " +
					"are mutually exclusive", source);
		}

		if (!StringUtils.hasText(endpoints)) {
			parserContext.getReaderContext().error("The 'endpoint' attribute is required", source);
		}

		BeanMetadataElement selectorBeanDefinition = null;
		if (hasSelector) {
			selectorBeanDefinition = new RuntimeBeanReference(selector);
		}
		else {
			BeanDefinitionBuilder selectorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(MetadataStoreSelector.class);
			BeanMetadataElement strategyBeanDefinition = null;
			if (hasStrategy) {
				strategyBeanDefinition = new RuntimeBeanReference(strategy);
			}
			else {
				strategyBeanDefinition =
						BeanDefinitionBuilder.genericBeanDefinition(ExpressionMetadataKeyStrategy.class)
								.addConstructorArgValue(expression)
								.getBeanDefinition();
			}
			selectorBuilder.addConstructorArgValue(strategyBeanDefinition);
			if (hasStore) {
				selectorBuilder.addConstructorArgReference(store);
			}
			selectorBeanDefinition = selectorBuilder.getBeanDefinition();
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(IdempotentReceiverInterceptor.class)
				.addConstructorArgValue(selectorBeanDefinition);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "throw-exception-on-rejection");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		AbstractBeanDefinition interceptorBeanDefinition = builder.getBeanDefinition();
		interceptorBeanDefinition.setAttribute(
				IdempotentReceiverAutoProxyCreatorIntegrationConfigurationInitializer.IDEMPOTENT_ENDPOINTS_MAPPING,
				endpoints);
		return interceptorBeanDefinition;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected boolean shouldFireEvents() {
		return false;
	}

}
