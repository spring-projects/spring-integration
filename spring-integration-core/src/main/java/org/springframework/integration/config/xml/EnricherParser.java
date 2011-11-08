/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.transformer.ContentEnricher;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'enricher' element.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public class EnricherParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(ContentEnricher.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");

		List<Element> propertyElements = DomUtils.getChildElementsByTagName(element, "property");
		if (!CollectionUtils.isEmpty(propertyElements)) {
			ManagedMap<String, Object> propertyExpressions = new ManagedMap<String, Object>();
			for (Element propertyElement : propertyElements) {
				String name = propertyElement.getAttribute("name");
				String value = propertyElement.getAttribute("value");
				String expression = propertyElement.getAttribute("expression");
				if (StringUtils.hasText(value) && StringUtils.hasText(expression)) {
					parserContext.getReaderContext().error("The 'value' and 'expression' attributes are mutually exclusive on " +
							"an <enricher> element's <property> sub-element.", parserContext.extractSource(propertyElement));
				}
				if (StringUtils.hasText(value)) {
					BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(LiteralExpression.class);
					expressionBuilder.addConstructorArgValue(value);
					propertyExpressions.put(name, expressionBuilder.getBeanDefinition());
				}
				else if (StringUtils.hasText(expression)) {
					BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
					expressionBuilder.addConstructorArgValue(expression);
					propertyExpressions.put(name, expressionBuilder.getBeanDefinition());
				}
				else {
					parserContext.getReaderContext().error("Exactly one of 'value' or 'expression' attributes must be provided on " +
							"an <enricher> element's <property> sub-element.", parserContext.extractSource(propertyElement));
				}
			}
			builder.addPropertyValue("propertyExpressions", propertyExpressions);
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
