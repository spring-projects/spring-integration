/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for {@code <int:barrier/>}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 */
public class BarrierParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = BeanDefinitionBuilder.genericBeanDefinition(BarrierMessageHandler.class);
		handlerBuilder.addConstructorArgValue(element.getAttribute("timeout"));
		String triggerTimeout = element.getAttribute("trigger-timeout");
		if (StringUtils.hasText(triggerTimeout)) {
			handlerBuilder.addConstructorArgValue(triggerTimeout);
		}
		String processor = element.getAttribute("output-processor");
		if (StringUtils.hasText(processor)) {
			handlerBuilder.addConstructorArgReference(processor);
		}
		IntegrationNamespaceUtils.injectConstructorWithAdapter("correlation-strategy",
				"correlation-strategy-method", "correlation-strategy-expression",
				"CorrelationStrategy", element, handlerBuilder, null, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "requires-reply");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "discard-channel");
		return handlerBuilder;
	}

}

