/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;scheduled-producer&gt; element.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class ScheduledProducerParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected String parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.endpoint.ExpressionEvaluatingMessageSource");
		String payloadExpression = element.getAttribute("payload-expression");
		RootBeanDefinition expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
		expressionDef.getConstructorArgumentValues().addGenericArgumentValue(payloadExpression);
		builder.addConstructorArgValue(expressionDef);
		builder.addConstructorArgValue(null); // TODO: add support for expectedType?
		this.parseHeaderExpressions(builder, element, parserContext);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

	private void parseHeaderExpressions(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "header");
		if (!CollectionUtils.isEmpty(headerElements)) {
			ManagedMap<String, Object> headerExpressions = new ManagedMap<String, Object>();
			for (Element headerElement : headerElements) {
				String headerName = headerElement.getAttribute("name");
				String headerValue = headerElement.getAttribute("value");
				String headerExpression = headerElement.getAttribute("expression");
				boolean hasValue = StringUtils.hasText(headerValue);
				boolean hasExpression = StringUtils.hasText(headerExpression);
				if (!(hasValue ^ hasExpression)) {
					parserContext.getReaderContext().error("exactly one of 'value' or 'expression' is required on a header sub-element",
							parserContext.extractSource(headerElement));
					continue;
				}
				RootBeanDefinition expressionDef = null;
				if (hasValue) {
					expressionDef = new RootBeanDefinition("org.springframework.expression.common.LiteralExpression");
					expressionDef.getConstructorArgumentValues().addGenericArgumentValue(headerValue);
				}
				else {
					expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
					expressionDef.getConstructorArgumentValues().addGenericArgumentValue(headerExpression);
				}
				headerExpressions.put(headerName, expressionDef);
			}
			builder.addPropertyValue("headerExpressions", headerExpressions);
		}
	}

}
