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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;resequencer&gt; element.
 * 
 * @author Marius Bogoevici
 */
public class ResequencerParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.Resequencer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		this.configureCorrelationStrategy(builder, element, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "release-partial-sequences");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-partial-result-on-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reaper-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "tracked-correlation-id-capacity");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		return builder;
	}

	private void configureCorrelationStrategy(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String ref = element.getAttribute("correlation-strategy");
		String method = element.getAttribute("correlation-strategy-method");
		String correlationStrategyProperty = "correlationStrategy";
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(method)) {
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						IntegrationNamespaceUtils.BASE_PACKAGE + ".aggregator.CorrelationStrategyAdapter");
				adapterBuilder.addConstructorArgReference(ref);
				adapterBuilder.getRawBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(method, "java.lang.String");
				String adapterBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
						adapterBuilder.getBeanDefinition(), parserContext.getRegistry());
				builder.addPropertyReference(correlationStrategyProperty, adapterBeanName);
			}
			else {
				builder.addPropertyReference(correlationStrategyProperty, ref);
			}
		}
	}

}
