/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'enricher' element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class EnricherParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ContentEnricher.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");

		List<Element> subElements = DomUtils.getChildElementsByTagName(element, "property");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");
				BeanDefinition beanDefinition = IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("value",
						"expression", parserContext, subElement, true);
				expressions.put(name, beanDefinition);
			}
			builder.addPropertyValue("propertyExpressions", expressions);
		}

		subElements = DomUtils.getChildElementsByTagName(element, "header");
		if (!CollectionUtils.isEmpty(subElements)) {
			ManagedMap<String, Object> expressions = new ManagedMap<String, Object>();
			for (Element subElement : subElements) {
				String name = subElement.getAttribute("name");
				BeanDefinition expressionDefinition = IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("value",
						"expression", parserContext, subElement, true);
				BeanDefinitionBuilder valueProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.support.ExpressionEvaluatingHeaderValueMessageProcessor");
				valueProcessorBuilder.addConstructorArgValue(expressionDefinition)
						.addConstructorArgValue(subElement.getAttribute("type"));
				IntegrationNamespaceUtils.setValueIfAttributeDefined(valueProcessorBuilder, subElement, "overwrite");
				expressions.put(name, valueProcessorBuilder.getBeanDefinition());
			}
			builder.addPropertyValue("headerExpressions", expressions);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "should-clone-payload");

		String requestPayloadExpression = element.getAttribute("request-payload-expression");

		if (StringUtils.hasText(requestPayloadExpression)) {
			BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(requestPayloadExpression);
			builder.addPropertyValue("requestPayloadExpression", expressionBuilder.getBeanDefinition());
		}

		return builder;
	}

}
