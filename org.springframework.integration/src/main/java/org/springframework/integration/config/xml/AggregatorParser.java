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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.CompletionStrategyAdapter;
import org.springframework.integration.aggregator.MethodInvokingAggregator;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace.
 * Registers the annotation-driven post-processors.
 * 
 * @author Marius Bogoevici
 * @author Mark Fisher
 */
public class AggregatorParser extends AbstractConsumerEndpointParser {

	private static final String COMPLETION_STRATEGY_REF_ATTRIBUTE = "completion-strategy";

	private static final String COMPLETION_STRATEGY_METHOD_ATTRIBUTE = "completion-strategy-method";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE = "send-partial-result-on-timeout";

	private static final String REAPER_INTERVAL_ATTRIBUTE = "reaper-interval";

	private static final String TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE = "tracked-correlation-id-capacity";

	private static final String TIMEOUT_ATTRIBUTE = "timeout";

	private static final String COMPLETION_STRATEGY_PROPERTY = "completionStrategy";


	@Override
	protected BeanDefinitionBuilder parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingAggregator.class);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.hasText(ref, "The '" + REF_ATTRIBUTE + "' attribute is required.");
		builder.addConstructorArgReference(ref);
		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.addConstructorArgValue(method);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, REAPER_INTERVAL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, TIMEOUT_ATTRIBUTE);
		final String completionStrategyRef = element.getAttribute(COMPLETION_STRATEGY_REF_ATTRIBUTE);
		final String completionStrategyMethod = element.getAttribute(COMPLETION_STRATEGY_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(completionStrategyRef)) {
			if (StringUtils.hasText(completionStrategyMethod)) {
				String adapterBeanName = this.createCompletionStrategyAdapter(
						completionStrategyRef, completionStrategyMethod, parserContext);
				builder.addPropertyReference(COMPLETION_STRATEGY_PROPERTY, adapterBeanName);	
			}
			else {
				builder.addPropertyReference(COMPLETION_STRATEGY_PROPERTY, completionStrategyRef);
			}
		}
		return builder;
	}

	private String createCompletionStrategyAdapter(String ref, String method, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CompletionStrategyAdapter.class);
		builder.addConstructorArgReference(ref);
		builder.addConstructorArgValue(method);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
