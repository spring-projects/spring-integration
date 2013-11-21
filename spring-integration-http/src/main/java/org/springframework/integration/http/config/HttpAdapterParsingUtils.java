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

package org.springframework.integration.http.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0.2
 */
abstract class HttpAdapterParsingUtils {

	static final String[] REST_TEMPLATE_REFERENCE_ATTRIBUTES = {
		"request-factory", "error-handler", "message-converters"
	};

	static void verifyNoRestTemplateAttributes(Element element, ParserContext parserContext) {
		for (String attributeName : REST_TEMPLATE_REFERENCE_ATTRIBUTES) {
			if (element.hasAttribute(attributeName)) {
				parserContext.getReaderContext().error("When providing a 'rest-template' reference, the '"
						+ attributeName + "' attribute is not allowed.",
						parserContext.extractSource(element));
			}
		}
	}

	static void configureUriVariableExpressions(BeanDefinitionBuilder builder, ParserContext parserContext, Element element) {
		String uriVariablesExpression = element.getAttribute("uri-variables-expression");

		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		boolean hasUriVariableExpressions = !CollectionUtils.isEmpty(uriVariableElements);

		if (StringUtils.hasText(uriVariablesExpression)) {
			if (hasUriVariableExpressions) {
				parserContext.getReaderContext().error("'uri-variables-expression' attribute " +
						"and 'uri-variable' sub-elements are mutually exclusive.", element);
			}
			BeanDefinitionBuilder beanDefinitionBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class)
					.addConstructorArgValue(uriVariablesExpression);
			builder.addPropertyValue("uriVariablesExpression", beanDefinitionBuilder.getBeanDefinition());
		}

		if (hasUriVariableExpressions) {
			ManagedMap<String, Object> uriVariableExpressions = new ManagedMap<String, Object>();
			for (Element uriVariableElement : uriVariableElements) {
				String name = uriVariableElement.getAttribute("name");
				String expression = uriVariableElement.getAttribute("expression");
				BeanDefinitionBuilder factoryBeanBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionFactoryBean.class);
				factoryBeanBuilder.addConstructorArgValue(expression);
				uriVariableExpressions.put(name,  factoryBeanBuilder.getBeanDefinition());
			}
			builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
		}
	}

	static void configureUrlConstructorArg(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String urlAttribute = element.getAttribute("url");
		String urlExpressionAttribute = element.getAttribute("url-expression");
		boolean hasUrlAttribute = StringUtils.hasText(urlAttribute);
		boolean hasUrlExpressionAttribute = StringUtils.hasText(urlExpressionAttribute);
		if (!(hasUrlAttribute ^ hasUrlExpressionAttribute)) {
			parserContext.getReaderContext().error("Adapter must have exactly one of 'url' or 'url-expression'", element);
		}
		RootBeanDefinition expressionDef;
		if (hasUrlAttribute) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(urlAttribute);
		}
		else {
			expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(urlExpressionAttribute);
		}
		builder.addConstructorArgValue(expressionDef);

	}

	static void setHttpMethodOrExpression(Element element, ParserContext parserContext, BeanDefinitionBuilder builder){
		String httpMethod = element.getAttribute("http-method");
		String httpMethodExpression = element.getAttribute("http-method-expression");

		boolean hasHttpMethod = StringUtils.hasText(httpMethod);
		boolean hasHttpMethodExpression = StringUtils.hasText(httpMethodExpression);

		if (hasHttpMethod && hasHttpMethodExpression){
			parserContext.getReaderContext().error("The 'http-method' and 'http-method-expression' are mutually exclusive. " +
					"You can only have one or the other", element);
		}

		RootBeanDefinition expressionDef = null;
		if (hasHttpMethod) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(httpMethod);
		}
		else if (hasHttpMethodExpression){
			expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(httpMethodExpression);
		}
		if (expressionDef != null){
			builder.addPropertyValue("httpMethodExpression", expressionDef);
		}
	}

	static void setExpectedResponseOrExpression(Element element, ParserContext parserContext, BeanDefinitionBuilder builder){
		String expectedResponseType = element.getAttribute("expected-response-type");
		String expectedResponseTypeExpression = element.getAttribute("expected-response-type-expression");

		boolean hasExpectedResponseType = StringUtils.hasText(expectedResponseType);
		boolean hasExpectedResponseTypeExpression = StringUtils.hasText(expectedResponseTypeExpression);

		if (hasExpectedResponseType && hasExpectedResponseTypeExpression){
			parserContext.getReaderContext().error("The 'expected-response-type' and 'expected-response-type-expression' are mutually exclusive. " +
					"You can only have one or the other", element);
		}

		RootBeanDefinition expressionDef = null;
		if (hasExpectedResponseType) {
			expressionDef = new RootBeanDefinition(LiteralExpression.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expectedResponseType);
		}
		else if (hasExpectedResponseTypeExpression){
			expressionDef = new RootBeanDefinition(ExpressionFactoryBean.class);
			expressionDef.getConstructorArgumentValues().addGenericArgumentValue(expectedResponseTypeExpression);
		}
		if (expressionDef != null){
			builder.addPropertyValue("expectedResponseTypeExpression", expressionDef);
		}
	}

}
