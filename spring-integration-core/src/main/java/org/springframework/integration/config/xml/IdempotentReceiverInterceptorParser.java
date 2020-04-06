/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.config.IdempotentReceiverAutoProxyCreatorInitializer;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.IdempotentReceiverInterceptor;
import org.springframework.integration.metadata.SimpleMetadataStore;
import org.springframework.integration.selector.MetadataStoreSelector;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;idempotent-receiver/&gt; element.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.1
 */
public class IdempotentReceiverInterceptorParser extends AbstractBeanDefinitionParser {

	@Override // NOSONAR complexity
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) { // NOSONAR
		Object source = parserContext.extractSource(element);

		String selector = element.getAttribute("selector");
		boolean hasSelector = StringUtils.hasText(selector);
		String store = element.getAttribute("metadata-store");
		boolean hasStore = StringUtils.hasText(store);
		String keyStrategy = element.getAttribute("key-strategy");
		boolean hasKeyStrategy = StringUtils.hasText(keyStrategy);
		String keyExpression = element.getAttribute("key-expression");
		boolean hasKeyExpression = StringUtils.hasText(keyExpression);
		String valueStrategy = element.getAttribute("value-strategy");
		boolean hasValueStrategy = StringUtils.hasText(valueStrategy);
		String valueExpression = element.getAttribute("value-expression");
		boolean hasValueExpression = StringUtils.hasText(valueExpression);
		String compareValues = element.getAttribute("compare-values");
		boolean hasCompareValues = StringUtils.hasText(compareValues);

		String endpoints = element.getAttribute("endpoint");

		if (!hasSelector && !(hasKeyStrategy || hasKeyExpression)) {
			parserContext.getReaderContext().error("One of the 'selector', 'key-strategy' or 'key-expression' " +
					"attributes must be provided", source);
		}

		if (hasSelector && (hasStore || hasKeyStrategy || hasKeyExpression || hasValueStrategy // NOSONAR complexity
				|| hasValueExpression || hasCompareValues)) {
			parserContext.getReaderContext().error("The 'selector' attribute is mutually exclusive with " +
					"'metadata-store', 'key-strategy', 'key-expression', 'value-strategy', " +
					"'value-expression', and 'compare-values'", source);
		}

		if (hasKeyStrategy && hasKeyExpression) {
			parserContext.getReaderContext().error("The 'key-strategy' and 'key-expression' attributes " +
					"are mutually exclusive", source);
		}

		if (hasValueStrategy && hasValueExpression) {
			parserContext.getReaderContext().error("The 'value-strategy' and 'value-expression' attributes " +
					"are mutually exclusive", source);
		}

		if (!StringUtils.hasText(endpoints)) {
			parserContext.getReaderContext().error("The 'endpoint' attribute is required", source);
		}

		BeanMetadataElement selectorBeanDefinition;
		if (hasSelector) {
			selectorBeanDefinition = new RuntimeBeanReference(selector);
		}
		else {
			BeanDefinitionBuilder selectorBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(MetadataStoreSelector.class);
			BeanMetadataElement keyStrategyBeanDefinition;
			if (hasKeyStrategy) {
				keyStrategyBeanDefinition = new RuntimeBeanReference(keyStrategy);
			}
			else {
				keyStrategyBeanDefinition =
						BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingMessageProcessor.class)
								.addConstructorArgValue(
										BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class).
												addConstructorArgValue(keyExpression)
												.getBeanDefinition()
								)
								.getBeanDefinition();
			}
			selectorBuilder.addConstructorArgValue(keyStrategyBeanDefinition);

			BeanMetadataElement valueStrategyBeanDefinition = null;
			if (hasValueStrategy) {
				valueStrategyBeanDefinition = new RuntimeBeanReference(valueStrategy);
			}
			else if (hasValueExpression) {
				valueStrategyBeanDefinition =
						BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingMessageProcessor.class)
								.addConstructorArgValue(
										BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class).
												addConstructorArgValue(valueExpression)
												.getBeanDefinition()
								)
								.getBeanDefinition();
			}
			selectorBuilder.addConstructorArgValue(valueStrategyBeanDefinition);

			if (hasStore) {
				selectorBuilder.addConstructorArgReference(store);
			}
			else {
				selectorBuilder.addConstructorArgValue(new RootBeanDefinition(SimpleMetadataStore.class));
			}
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(selectorBuilder, element, "compare-values");
			selectorBeanDefinition = selectorBuilder.getBeanDefinition();
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(IdempotentReceiverInterceptor.class)
				.addConstructorArgValue(selectorBeanDefinition);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "throw-exception-on-rejection");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		AbstractBeanDefinition interceptorBeanDefinition = builder.getBeanDefinition();
		interceptorBeanDefinition.setAttribute(
				IdempotentReceiverAutoProxyCreatorInitializer.IDEMPOTENT_ENDPOINTS_MAPPING,
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
