/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 * @author Dave Syer
 */
public class ResequencerParser extends AbstractConsumerEndpointParser {

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String CORRELATION_STRATEGY_ATTRIBUTE = "correlation-strategy";

	private static final String SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE = "send-partial-result-on-expiry";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String MESSAGE_STORE_ATTRIBUTE = "message-store";

	private static final String COMPARATOR_ATTRIBUTE = "comparator";

	private static final String RELEASE_STRATEGY_REF_ATTRIBUTE = "release-strategy";

	private static final String RELEASE_STRATEGY_METHOD_ATTRIBUTE = "release-strategy-method";

	private static final String RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE = "release-partial-sequences";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.CorrelatingMessageHandler");
		BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
						+ ".aggregator.ResequencingMessageGroupProcessor");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(processorBuilder, element, COMPARATOR_ATTRIBUTE);

		String processorRef = BeanDefinitionReaderUtils.registerWithGeneratedName(processorBuilder.getBeanDefinition(),
				parserContext.getRegistry());

		// Message group processor
		builder.addConstructorArgReference(processorRef);

		// Message store
		builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".store.SimpleMessageStore").getBeanDefinition());

		String correlationStrategyRef = getCorrelationStrategyRef(element, parserContext);
		if (correlationStrategyRef != null) {
			builder.addConstructorArgReference(correlationStrategyRef);
		} else {
			// Correlation strategy
			builder.addConstructorArgValue(null);
		}

		// Release strategy
		builder.addConstructorArgValue(getReleaseStrategy(element, parserContext));

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_STORE_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder;
	}

	private Object getReleaseStrategy(Element element, ParserContext parserContext) {
		String releaseStrategyRef = getReleasenStrategyRef(element, parserContext);
		if (releaseStrategyRef == null) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
							+ ".aggregator.SequenceSizeReleaseStrategy");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE);
			return builder.getBeanDefinition();
		}
		if (StringUtils.hasText(element.getAttribute(RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE))) {
			parserContext.getReaderContext().error(
					"Only one of " + RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE + " and " + RELEASE_STRATEGY_REF_ATTRIBUTE
							+ " can be specified at once", element);
		}
		return new RuntimeBeanReference(releaseStrategyRef);
	}

	private String getCorrelationStrategyRef(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(CORRELATION_STRATEGY_ATTRIBUTE);
		String method = element.getAttribute(CORRELATION_STRATEGY_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(method)) {
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
								+ ".aggregator.CorrelationStrategyAdapter");
				adapterBuilder.addConstructorArgReference(ref);
				adapterBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method,
						"java.lang.String");
				String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
						.getBeanDefinition(), parserContext.getRegistry());
				return adapterBeanName;
			} else {
				return ref;
			}
		}
		return null;
	}

	private String getReleasenStrategyRef(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(RELEASE_STRATEGY_REF_ATTRIBUTE);
		String method = element.getAttribute(RELEASE_STRATEGY_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(method)) {
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
								+ ".aggregator.ReleaseStrategyAdapter");
				adapterBuilder.addConstructorArgReference(ref);
				adapterBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method,
						"java.lang.String");
				String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
						.getBeanDefinition(), parserContext.getRegistry());
				return adapterBeanName;
			} else {
				return ref;
			}
		}
		return null;
	}

}
