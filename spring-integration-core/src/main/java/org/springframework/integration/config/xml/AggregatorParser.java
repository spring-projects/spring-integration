/*
 * Copyright 2002-2010 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.AggregatingMessageHandler;
import org.springframework.integration.aggregator.MethodInvokingMessageGroupProcessor;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace. Registers the annotation-driven
 * post-processors.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 */
public class AggregatorParser extends AbstractConsumerEndpointParser {

	private static final String RELEASE_STRATEGY_REF_ATTRIBUTE = "release-strategy";

	private static final String RELEASE_STRATEGY_METHOD_ATTRIBUTE = "release-strategy-method";

	private static final String RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE = "release-strategy-expression";

	private static final String CORRELATION_STRATEGY_REF_ATTRIBUTE = "correlation-strategy";

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE = "correlation-strategy-expression";

	private static final String MESSAGE_STORE_ATTRIBUTE = "message-store";

	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE = "send-partial-result-on-expiry";

	private static final String RELEASE_STRATEGY_PROPERTY = "releaseStrategy";

	private static final String CORRELATION_STRATEGY_PROPERTY = "correlationStrategy";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanComponentDefinition innerHandlerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element,
				parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		BeanDefinitionBuilder builder;

		builder = BeanDefinitionBuilder.genericBeanDefinition(AggregatingMessageHandler.class);
		BeanDefinitionBuilder processorBuilder = null;
		BeanMetadataElement processor = null;

		if (innerHandlerDefinition != null || StringUtils.hasText(ref)) {
			processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingMessageGroupProcessor.class);
			builder.addConstructorArgValue(processorBuilder.getBeanDefinition());
			if (innerHandlerDefinition != null) {
				processor = innerHandlerDefinition;
			}
			else {
				processor = new RuntimeBeanReference(ref);
			}
			processorBuilder.addConstructorArgValue(processor);
		}
		else {
			if (StringUtils.hasText(element.getAttribute(EXPRESSION_ATTRIBUTE))) {
				String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
						+ ".aggregator.ExpressionEvaluatingMessageGroupProcessor");
				adapterBuilder.addConstructorArgValue(expression);
				builder.addConstructorArgValue(adapterBuilder.getBeanDefinition());
			}
			else {
				builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
						IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.DefaultAggregatingMessageGroupProcessor")
						.getBeanDefinition());
			}
		}

		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			processorBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method,
					"java.lang.String");
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_STORE_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		this.injectPropertyWithAdapter(RELEASE_STRATEGY_REF_ATTRIBUTE, RELEASE_STRATEGY_METHOD_ATTRIBUTE,
				RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE, RELEASE_STRATEGY_PROPERTY, "ReleaseStrategy", element, builder,
				processor, parserContext);
		this.injectPropertyWithAdapter(CORRELATION_STRATEGY_REF_ATTRIBUTE, CORRELATION_STRATEGY_METHOD_ATTRIBUTE,
				CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE, CORRELATION_STRATEGY_PROPERTY, "CorrelationStrategy",
				element, builder, processor, parserContext);
		return builder;
	}

	private void injectPropertyWithAdapter(String beanRefAttribute, String methodRefAttribute,
			String expressionAttribute, String beanProperty, String adapterClass, Element element,
			BeanDefinitionBuilder builder, BeanMetadataElement processor, ParserContext parserContext) {
		final String beanRef = element.getAttribute(beanRefAttribute);
		final String beanMethod = element.getAttribute(methodRefAttribute);
		final String expression = element.getAttribute(expressionAttribute);
		BeanMetadataElement adapter = null;
		if (StringUtils.hasText(beanRef)) {
			adapter = this.createAdapter(new RuntimeBeanReference(beanRef), beanMethod, adapterClass, parserContext);
		}
		else if (processor != null) {
			adapter = this.createAdapter(processor, beanMethod, adapterClass, parserContext);
		}
		else if (StringUtils.hasText(expression)) {
			BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.ExpressionEvaluating"
							+ adapterClass);
			adapterBuilder.addConstructorArgValue(expression);
			adapter = adapterBuilder.getBeanDefinition();
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
