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

import org.w3c.dom.Element;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace.
 * Registers the annotation-driven post-processors.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class AggregatorParser extends AbstractConsumerEndpointParser {

	private static final String COMPLETION_STRATEGY_REF_ATTRIBUTE = "completion-strategy";

	private static final String COMPLETION_STRATEGY_METHOD_ATTRIBUTE = "completion-strategy-method";

	private static final String CORRELATION_STRATEGY_REF_ATTRIBUTE = "correlation-strategy";

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String OUTPUT_CHANNEL_ATTRIBUTE = "output-channel";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE = "send-partial-result-on-timeout";

	private static final String REAPER_INTERVAL_ATTRIBUTE = "reaper-interval";

	//private static final String TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE = "tracked-correlation-id-capacity";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String AUTO_STARTUP_ATTRIBUTE = "auto-startup";

	private static final String COMPLETION_STRATEGY_PROPERTY = "completionStrategy";

	private static final String CORRELATION_STRATEGY_PROPERTY = "correlationStrategy";


	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanComponentDefinition innerHandlerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(
				element, parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.CorrelatingMessageHandler");

		BeanDefinitionBuilder processorBuilder = null;
		if (innerHandlerDefinition != null || StringUtils.hasText(ref)) {
			processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.MethodInvokingMessageGroupProcessor");
			if (innerHandlerDefinition != null) {
				processorBuilder.addConstructorArgValue(innerHandlerDefinition);
			}
			else {
				processorBuilder.addConstructorArgReference(ref);
			}
			builder.addConstructorArgValue(processorBuilder.getBeanDefinition());
		}
		else {
			builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.DefaultAggregatingMessageGroupProcessor")
					.getBeanDefinition());
		}

		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			processorBuilder.getRawBeanDefinition().getConstructorArgumentValues()
					.addGenericArgumentValue(method, "java.lang.String");
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(
				builder, element, SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REAPER_INTERVAL_ATTRIBUTE);
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,
		// element, TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, AUTO_STARTUP_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, TIMEOUT_ATTRIBUTE);
		this.injectPropertyWithBean(COMPLETION_STRATEGY_REF_ATTRIBUTE, COMPLETION_STRATEGY_METHOD_ATTRIBUTE,
				COMPLETION_STRATEGY_PROPERTY, "CompletionStrategyAdapter", element, builder, parserContext);
		this.injectPropertyWithBean(CORRELATION_STRATEGY_REF_ATTRIBUTE, CORRELATION_STRATEGY_METHOD_ATTRIBUTE,
				CORRELATION_STRATEGY_PROPERTY, "CorrelationStrategyAdapter", element, builder, parserContext);
		return builder;
	}

	private void injectPropertyWithBean(String beanRefAttribute, String methodRefAttribute, String beanProperty,
			String adapterClass, Element element, BeanDefinitionBuilder builder, ParserContext parserContext) {
		final String beanRef = element.getAttribute(beanRefAttribute);
		final String beanMethod = element.getAttribute(methodRefAttribute);
		if (StringUtils.hasText(beanRef)) {
			if (StringUtils.hasText(beanMethod)) {
				String adapterBeanName = this.createAdapter(beanRef, beanMethod, adapterClass, parserContext);
				builder.addPropertyReference(beanProperty, adapterBeanName);
			}
			else {
				builder.addPropertyReference(beanProperty, beanRef);
			}
		}
	}

	private String createAdapter(String ref, String method, String unqualifiedClassName, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator." + unqualifiedClassName);
		builder.addConstructorArgReference(ref);
		builder.getRawBeanDefinition().getConstructorArgumentValues()
				.addGenericArgumentValue(method, "java.lang.String");
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
