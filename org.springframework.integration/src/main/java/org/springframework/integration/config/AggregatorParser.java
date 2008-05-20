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
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.router.AggregatingMessageHandler;
import org.springframework.integration.router.AggregatorAdapter;
import org.springframework.integration.router.CompletionStrategyAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace.
 * Registers the annotation-driven post-processors.
 * 
 * @author Marius Bogoevici
 */
public class AggregatorParser implements BeanDefinitionParser {

	public static final String ID_ATTRIBUTE = "id";

	public static final String REF_ATTRIBUTE = "ref";

	public static final String METHOD_ATTRIBUTE = "method";

	public static final String COMPLETION_STRATEGY_ATTRIBUTE = "completion-strategy";

	public static final String DEFAULT_REPLY_CHANNEL_ATTRIBUTE = "default-reply-channel";

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

	public static final String COMPLETION_STRATEGY_ELEMENT = "completion-strategy";


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		return parseAggregatorElement(element, parserContext, true);
	}

	private BeanDefinition parseAggregatorElement(Element element, ParserContext parserContext, boolean topLevel) {
		final RootBeanDefinition aggregatorDef = new RootBeanDefinition(AggregatingMessageHandler.class);
		aggregatorDef.setSource(parserContext.extractSource(element));
		final String id = element.getAttribute(ID_ATTRIBUTE);
		final String ref = element.getAttribute(REF_ATTRIBUTE);
		final String method = element.getAttribute(METHOD_ATTRIBUTE);
		final String completionStrategyRef = element.getAttribute(COMPLETION_STRATEGY_ATTRIBUTE);
		final NodeList completionStrategyChildElements = element.getElementsByTagName(COMPLETION_STRATEGY_ELEMENT);
		if (!StringUtils.hasText(ref)) {
			throw new ConfigurationException("The 'ref' attribute must be present");
		}
		if (!topLevel && StringUtils.hasText(id)) {
			parserContext.getReaderContext().error(
					"The 'id' attribute is only supported for top-level <aggregator> elements.",
					parserContext.extractSource(element));
		}
		if (completionStrategyChildElements.getLength() > 0 && StringUtils.hasText(completionStrategyRef)) {
			parserContext
					.getReaderContext()
					.error(
							"The 'completion-strategy' element is only supported when no 'completion-strategy' attribute is specified.",
							parserContext.extractSource(element));
		}
		if (!StringUtils.hasText(method)) {
			aggregatorDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(ref));
		}
		else {
			String adapterBeanName = createAdapterAndReturnBeanName(parserContext, ref, method, AggregatorAdapter.class);
			aggregatorDef.getConstructorArgumentValues().addGenericArgumentValue(
					new RuntimeBeanReference(adapterBeanName));
		}

		if (StringUtils.hasText(completionStrategyRef)) {
			aggregatorDef.getPropertyValues().addPropertyValue(COMPLETION_STRATEGY_PROPERTY,
					new RuntimeBeanReference(completionStrategyRef));
		}
		else if (completionStrategyChildElements.getLength() > 0) {
			Element completionStrategyElement = (Element) completionStrategyChildElements.item(0);
			String childCompletionStrategyReference = completionStrategyElement.getAttribute(REF_ATTRIBUTE);
			String childCompletionStrategyMethod = completionStrategyElement.getAttribute(METHOD_ATTRIBUTE);
			String adapterBeanName = createAdapterAndReturnBeanName(parserContext, childCompletionStrategyReference,
					childCompletionStrategyMethod, CompletionStrategyAdapter.class);
			aggregatorDef.getPropertyValues().addPropertyValue(COMPLETION_STRATEGY_PROPERTY,
					new RuntimeBeanReference(adapterBeanName));
		}

		IntegrationNamespaceUtils.setBeanReferenceIfAttributeDefined(aggregatorDef, DEFAULT_REPLY_CHANNEL_PROPERTY,
				element, DEFAULT_REPLY_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setBeanReferenceIfAttributeDefined(aggregatorDef, DISCARD_CHANNEL_PROPERTY, element,
				DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(aggregatorDef, SEND_TIMEOUT_PROPERTY, element,
				SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(aggregatorDef, SEND_PARTIAL_RESULT_ON_TIMEOUT_PROPERTY,
				element, SEND_PARTIAL_RESULT_ON_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(aggregatorDef, REAPER_INTERVAL_PROPERTY, element,
				REAPER_INTERVAL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(aggregatorDef, TRACKED_CORRELATION_ID_CAPACITY_PROPERTY,
				element, TRACKED_CORRELATION_ID_CAPACITY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(aggregatorDef, TIMEOUT, element, TIMEOUT_ATTRIBUTE);
		String beanName = StringUtils.hasText(id) ? id : parserContext.getReaderContext().generateBeanName(
				aggregatorDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(aggregatorDef, beanName));
		return aggregatorDef;
	}

	private String createAdapterAndReturnBeanName(ParserContext parserContext, final String ref, final String method,
			Class<?> adapterClass) {
		BeanDefinition adapterDefinition = new RootBeanDefinition(adapterClass);
		adapterDefinition.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(ref));
		adapterDefinition.getConstructorArgumentValues().addGenericArgumentValue(method);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDefinition);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDefinition, adapterBeanName));
		return adapterBeanName;
	}

}
