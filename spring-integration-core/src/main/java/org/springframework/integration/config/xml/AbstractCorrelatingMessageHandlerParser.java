/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.springframework.integration.config.xml;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.AbstractCorrelatingMessageHandler;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Base class for parsers that create an instance of {@link AbstractCorrelatingMessageHandler}
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 *
 */
public abstract class AbstractCorrelatingMessageHandlerParser extends AbstractConsumerEndpointParser {

	private static final String CORRELATION_STRATEGY_REF_ATTRIBUTE = "correlation-strategy";

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE = "correlation-strategy-expression";

	private static final String CORRELATION_STRATEGY_PROPERTY = "correlationStrategy";

	private static final String MESSAGE_STORE_ATTRIBUTE = "message-store";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE = "send-partial-result-on-expiry";

	protected void doParse(BeanDefinitionBuilder builder, Element element, BeanMetadataElement processor, ParserContext parserContext){
		this.injectPropertyWithAdapter(CORRELATION_STRATEGY_REF_ATTRIBUTE, CORRELATION_STRATEGY_METHOD_ATTRIBUTE,
				CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE, CORRELATION_STRATEGY_PROPERTY, "CorrelationStrategy",
				element, builder, processor, parserContext);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_STORE_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE);
	}

	protected void injectPropertyWithAdapter(String beanRefAttribute, String methodRefAttribute,
			String expressionAttribute, String beanProperty, String adapterClass, Element element,
			BeanDefinitionBuilder builder, BeanMetadataElement processor, ParserContext parserContext) {
		
		final String beanRef = element.getAttribute(beanRefAttribute);
		final String beanMethod = element.getAttribute(methodRefAttribute);
		final String expression = element.getAttribute(expressionAttribute);
		
		final boolean hasBeanRef = StringUtils.hasText(beanRef);
		final boolean hasExpression = StringUtils.hasText(expression);
		
		if (hasBeanRef && hasExpression) {
			parserContext.getReaderContext().error(
					"Exactly one of the '" + beanRefAttribute + "' or '" + expressionAttribute +
					"' attribute is allowed.", element);
		}
		
		BeanMetadataElement adapter = null;
		if (hasBeanRef) {
			adapter = this.createAdapter(new RuntimeBeanReference(beanRef), beanMethod, adapterClass, parserContext);
		}
		else if (hasExpression) {
			BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.ExpressionEvaluating"
							+ adapterClass);
			adapterBuilder.addConstructorArgValue(expression);
			adapter = adapterBuilder.getBeanDefinition();
		}
		else if (processor != null) {
			adapter = this.createAdapter(processor, beanMethod, adapterClass, parserContext);
		}
		else {
			adapter = this.createAdapter(null, beanMethod, adapterClass, parserContext);
		}
		builder.addPropertyValue(beanProperty, adapter);
	}

	private BeanMetadataElement createAdapter(BeanMetadataElement ref, String method, String unqualifiedClassName,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".config." + unqualifiedClassName
						+ "FactoryBean");
		builder.addConstructorArgValue(ref);
		if (StringUtils.hasText(method)) {
			builder.addConstructorArgValue(method);
		}
		return builder.getBeanDefinition();
	}
}
