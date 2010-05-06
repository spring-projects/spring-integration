/*
 * Copyright 2002-2009 the original author or authors.
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

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.CorrelatingMessageHandler");
		BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.Resequencer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(processorBuilder, element, "release-partial-sequences");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(processorBuilder, element, "comparator");

		String processorRef = BeanDefinitionReaderUtils.registerWithGeneratedName(processorBuilder
				.getBeanDefinition(), parserContext.getRegistry());

		// Message group processor
		builder.addConstructorArgReference(processorRef);

		// Message store
		builder.addConstructorArgValue(BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".store.SimpleMessageStore").getBeanDefinition());

		String correlationStrategyRef = getCorrelationStrategyRef(element, parserContext);
		if (correlationStrategyRef != null) {
			builder.addConstructorArgReference(correlationStrategyRef);
		}
		else {
			// Correlation strategy
			builder.addConstructorArgValue(null);
		}
		// Release strategy
		builder.addConstructorArgReference(processorRef);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-store");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-partial-result-on-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder;
	}

	private String getCorrelationStrategyRef(Element element, ParserContext parserContext) {
		String ref = element.getAttribute("correlation-strategy");
		String method = element.getAttribute("correlation-strategy-method");
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
			}
			else {
				return ref;
			}
		}
		return null;
	}

}
