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
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.ResequencingMessageGroupProcessor;
import org.springframework.integration.aggregator.ResequencingMessageHandler;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 * @author Dave Syer
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public class ResequencerParser extends AbstractConsumerEndpointParser {

	private static final String CORRELATION_STRATEGY_REF_ATTRIBUTE = "correlation-strategy";

	private static final String CORRELATION_STRATEGY_METHOD_ATTRIBUTE = "correlation-strategy-method";

	private static final String CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE = "correlation-strategy-expression";

	private static final String SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE = "send-partial-result-on-expiry";

	private static final String SEND_TIMEOUT_ATTRIBUTE = "send-timeout";

	private static final String DISCARD_CHANNEL_ATTRIBUTE = "discard-channel";

	private static final String MESSAGE_STORE_ATTRIBUTE = "message-store";

	private static final String COMPARATOR_REF_ATTRIBUTE = "comparator";

	private static final String RELEASE_STRATEGY_REF_ATTRIBUTE = "release-strategy";

	private static final String RELEASE_STRATEGY_METHOD_ATTRIBUTE = "release-strategy-method";

	private static final String RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE = "release-strategy-expression";

	private static final String RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE = "release-partial-sequences";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageHandler.class);
		BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(ResequencingMessageGroupProcessor.class);

		// Comparator
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(processorBuilder, element, COMPARATOR_REF_ATTRIBUTE);

		String processorRef = BeanDefinitionReaderUtils.registerWithGeneratedName(processorBuilder.getBeanDefinition(),
				parserContext.getRegistry());

		// Message group processor
		builder.addConstructorArgReference(processorRef);

		// Message store
		builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".store.SimpleMessageStore").getBeanDefinition());

		// Correlation strategy
		builder.addConstructorArgValue(getCorrelationStrategy(element, parserContext));
		// Release strategy
		builder.addConstructorArgValue(getReleaseStrategy(element, parserContext));

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, MESSAGE_STORE_ATTRIBUTE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, DISCARD_CHANNEL_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_TIMEOUT_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, SEND_PARTIAL_RESULT_ON_EXPIRY_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder;
	}

	private BeanMetadataElement getCorrelationStrategy(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(CORRELATION_STRATEGY_REF_ATTRIBUTE);
		String expression = element.getAttribute(CORRELATION_STRATEGY_EXPRESSION_ATTRIBUTE);
		String method = element.getAttribute(CORRELATION_STRATEGY_METHOD_ATTRIBUTE);
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(expression)) {
				parserContext.getReaderContext().error(
						"Only one of correlation strategy expression and bean reference must be specified", element);
				return null;
			}
			if (StringUtils.hasText(method)) {
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
								+ ".aggregator.MethodInvokingCorrelationStrategy");
				adapterBuilder.addConstructorArgReference(ref);
				adapterBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method,
						"java.lang.String");
				String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
						.getBeanDefinition(), parserContext.getRegistry());
				return new RuntimeBeanReference(adapterBeanName);
			}
			else {
				return new RuntimeBeanReference(ref);
			}
		}
		else {
			if (!StringUtils.hasText(expression)) {
				return null;
			}
			BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
							+ ".aggregator.ExpressionEvaluatingCorrelationStrategy");
			adapterBuilder.addConstructorArgValue(expression);
			String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
					.getBeanDefinition(), parserContext.getRegistry());
			return new RuntimeBeanReference(adapterBeanName);
		}
	}

	private BeanMetadataElement getReleaseStrategy(Element element, ParserContext parserContext) {
		String ref = element.getAttribute(RELEASE_STRATEGY_REF_ATTRIBUTE);
		String method = element.getAttribute(RELEASE_STRATEGY_METHOD_ATTRIBUTE);
		String expression = element.getAttribute(RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE);
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(expression)) {
				parserContext.getReaderContext().error(
						"Only one of release strategy expression and bean reference must be specified", element);
				return null;
			}
			if (StringUtils.hasText(element.getAttribute(RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE))) {
				parserContext.getReaderContext().error(
						"Only one of " + RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE + " and " + RELEASE_STRATEGY_REF_ATTRIBUTE
								+ " can be specified at once", element);
				return null;
			}
			if (StringUtils.hasText(method)) {
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
								+ ".aggregator.MethodInvokingReleaseStrategy");
				adapterBuilder.addConstructorArgReference(ref);
				adapterBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method,
						"java.lang.String");
				String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
						.getBeanDefinition(), parserContext.getRegistry());
				return new RuntimeBeanReference(adapterBeanName);
			}
			else {
				return new RuntimeBeanReference(ref);
			}
		}
		else {
			if (!StringUtils.hasText(expression)) {
				return null;
			}
			if (StringUtils.hasText(element.getAttribute(RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE))) {
				parserContext.getReaderContext().error(
						"Only one of " + RELEASE_PARTIAL_SEQUENCES_ATTRIBUTE + " and "
								+ RELEASE_STRATEGY_EXPRESSION_ATTRIBUTE + " can be specified at once", element);
				return null;
			}
			BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE
							+ ".aggregator.ExpressionEvaluatingReleaseStrategy");
			adapterBuilder.addConstructorArgValue(expression);
			String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(adapterBuilder
					.getBeanDefinition(), parserContext.getRegistry());
			return new RuntimeBeanReference(adapterBeanName);
		}
	}

}
