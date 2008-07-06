/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.AggregatorAdapter;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace.
 * Registers the annotation-driven post-processors.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregatorParser extends AbstractHandlerEndpointParser {

	public static final String COMPLETION_STRATEGY_REF_ATTRIBUTE = "completion-strategy";

	public static final String COMPLETION_STRATEGY_METHOD_ATTRIBUTE = "completion-strategy-method";

	public static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	public static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	public static final String SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE = "send-partial-result-on-timeout";

	public static final String REAPER_INTERVAL_ATTRIBUTE = "reaper-interval";

	public static final String TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE = "tracked-correlation-id-capacity";

	public static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String COMPLETION_STRATEGY_PROPERTY = "completionStrategy";

	private static final String DEFAULT_REPLY_CHANNEL_PROPERTY = "defaultReplyChannel";

	private static final String DISCARD_CHANNEL_PROPERTY = "discardChannel";

	private static final String SEND_TIMEOUT_PROPERTY = "sendTimeout";

	private static final String SEND_PARTIAL_RESULT_ON_TIMEOUT_PROPERTY = "sendPartialResultOnTimeout";

	private static final String REAPER_INTERVAL_PROPERTY = "reaperInterval";

	public static final String TRACKED_CORRELATION_ID_CAPACITY_PROPERTY = "trackedCorrelationIdCapacity";

	public static final String TIMEOUT = "timeout";

	public static final String AGGREGATOR_ELEMENT = "aggregator";


	@Override
	protected Class<? extends MessageHandler> getHandlerAdapterClass() {
		return AggregatingMessageHandler.class;
	}

	@Override
	protected boolean shouldCreateAdapter(Element element) {
		return true;
	}

	@Override
	protected String parseAdapter(String ref, String method, Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getHandlerAdapterClass());
		if (StringUtils.hasText(method)) {
			String aggregatorAdapterBeanName = this.createAdapter(ref, method, parserContext, AggregatorAdapter.class);
			builder.addConstructorArgReference(aggregatorAdapterBeanName);
		}
		else {
			builder.addConstructorArgReference(ref);
		}
		final String completionStrategyRef = element.getAttribute(COMPLETION_STRATEGY_REF_ATTRIBUTE);
		final String completionStrategyMethod = element.getAttribute(COMPLETION_STRATEGY_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(completionStrategyRef)) {
			if (StringUtils.hasText(completionStrategyMethod)) {
				String adapterBeanName = this.createAdapter(completionStrategyRef,
						completionStrategyMethod, parserContext, CompletionStrategyAdapter.class);
				builder.addPropertyReference(COMPLETION_STRATEGY_PROPERTY, adapterBeanName);	
			}
			else {
				builder.addPropertyReference(COMPLETION_STRATEGY_PROPERTY, completionStrategyRef);
			}
		}
		IntegrationNamespaceUtils.setBeanReferenceIfAttributeDefined(builder, DEFAULT_REPLY_CHANNEL_PROPERTY,
				element, OUTPUT_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setBeanReferenceIfAttributeDefined(builder, DISCARD_CHANNEL_PROPERTY, element,
				DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, SEND_TIMEOUT_PROPERTY, element,
				SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, SEND_PARTIAL_RESULT_ON_TIMEOUT_PROPERTY,
				element, SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, REAPER_INTERVAL_PROPERTY, element,
				REAPER_INTERVAL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, TRACKED_CORRELATION_ID_CAPACITY_PROPERTY,
				element, TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, TIMEOUT, element, TIMEOUT_ATTRIBUTE);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private String createAdapter(String ref, String method, ParserContext parserContext, Class<?> adapterClass) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(adapterClass);
		builder.addConstructorArgReference(ref);
		builder.addConstructorArgValue(method);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
