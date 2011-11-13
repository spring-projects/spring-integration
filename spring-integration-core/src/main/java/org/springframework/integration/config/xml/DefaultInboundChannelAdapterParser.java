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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;inbound-channel-adapter/&gt; element.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public class DefaultInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanMetadataElement result = null;
		BeanComponentDefinition innnerBeanDef = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String sourceRef = element.getAttribute("ref");
		String methodName = element.getAttribute("method");
		String expressionString = element.getAttribute("expression");
//		TODO Add check for mutually exclusive options: inner or ref, or expression and expression without method. @see  DefaultOutboundChannelAdapterParser
		if (innnerBeanDef != null) {
			if (StringUtils.hasText(sourceRef)) {
				parserContext.getReaderContext().error(
						"inner bean and a 'ref' attribute are mutually exclusive options", element);
			}
			if (StringUtils.hasText(methodName)) {
				result = this.parseMethodInvokingSource(innnerBeanDef, methodName, element, parserContext);
			}
			else {
				result = innnerBeanDef;
			}
		}
		else if (StringUtils.hasText(expressionString)) {
			if (StringUtils.hasText(sourceRef)) {
				parserContext.getReaderContext().error(
						"the 'expression' and 'ref' attributes are mutually exclusive options", element);
			}
			String expressionBeanName = this.parseExpression(expressionString, element, parserContext);
			result = new RuntimeBeanReference(expressionBeanName);
		}
		else if (StringUtils.hasText(sourceRef)) {
			BeanMetadataElement sourceValue = new RuntimeBeanReference(sourceRef);
			if (StringUtils.hasText(methodName)) {
				result = this.parseMethodInvokingSource(sourceValue, methodName, element, parserContext);
			}
			else {
				result = sourceValue;
			}
		}
		else {
			parserContext.getReaderContext().error("One of the following is required: " +
					"'ref' attribute, 'expression' attribute, or an inner-bean definition.", element);
		}
		return result;
	}

	private BeanMetadataElement parseMethodInvokingSource(BeanMetadataElement targetObject, String methodName, Element element, ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".endpoint.MethodInvokingMessageSource");
		sourceBuilder.addPropertyValue("object", targetObject);
		sourceBuilder.addPropertyValue("methodName", methodName);
		this.parseHeaderExpressions(sourceBuilder, element, parserContext);
		String sourceRef = BeanDefinitionReaderUtils.registerWithGeneratedName(
				sourceBuilder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(sourceRef);
	}

	private String parseExpression(String expressionString, Element element, ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.endpoint.ExpressionEvaluatingMessageSource");
		RootBeanDefinition expressionDef = new RootBeanDefinition("org.springframework.integration.config.ExpressionFactoryBean");
		expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expressionString);
		sourceBuilder.addConstructorArgValue(expressionDef);
		sourceBuilder.addConstructorArgValue(null); // TODO: add support for expectedType?
		this.parseHeaderExpressions(sourceBuilder, element, parserContext);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(sourceBuilder.getBeanDefinition(), parserContext.getRegistry());
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
