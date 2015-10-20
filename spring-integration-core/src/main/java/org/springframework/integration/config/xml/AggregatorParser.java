/*
 * Copyright 2002-2015 the original author or authors.
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.ExpressionEvaluatingMessageGroupProcessor;
import org.springframework.integration.config.AggregatorFactoryBean;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>aggregator</em> element of the integration namespace. Registers the annotation-driven
 * post-processors.
 *
 * @author Marius Bogoevici
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Stefan Ferstl
 * @author Gary Russell
 */
public class AggregatorParser extends AbstractCorrelatingMessageHandlerParser {

	private static final String EXPIRE_GROUPS_UPON_COMPLETION = "expire-groups-upon-completion";

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanComponentDefinition innerHandlerDefinition = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element,
				parserContext);
		String ref = element.getAttribute(REF_ATTRIBUTE);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(AggregatorFactoryBean.class);
		BeanMetadataElement processor = null;

		if (innerHandlerDefinition != null || StringUtils.hasText(ref)) {
			if (innerHandlerDefinition != null) {
				processor = innerHandlerDefinition;
			}
			else {
				processor = new RuntimeBeanReference(ref);
			}
			builder.addPropertyValue("processorBean", processor);
		}
		else {
			if (StringUtils.hasText(element.getAttribute(EXPRESSION_ATTRIBUTE))) {
				String expression = element.getAttribute(EXPRESSION_ATTRIBUTE);
				BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(ExpressionEvaluatingMessageGroupProcessor.class);
				adapterBuilder.addConstructorArgValue(expression);
				builder.addPropertyValue("processorBean", adapterBuilder.getBeanDefinition());
			}
			else {
				builder.addPropertyValue("processorBean", BeanDefinitionBuilder
						.genericBeanDefinition(DefaultAggregatingMessageGroupProcessor.class).getBeanDefinition());
			}
		}

		if (StringUtils.hasText(element.getAttribute(METHOD_ATTRIBUTE))) {
			String method = element.getAttribute(METHOD_ATTRIBUTE);
			builder.addPropertyValue("methodName", method);
		}

		this.doParse(builder, element, processor, parserContext);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXPIRE_GROUPS_UPON_COMPLETION);

		return builder;
	}
}
