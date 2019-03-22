/*
 * Copyright 2002-2019 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.integration.config.ExpressionFactoryBean;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;wire-tap&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class WireTapParser implements BeanDefinitionRegisteringParser {

	@Override
	public String parse(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(WireTap.class);
		String targetRef = element.getAttribute("channel");
		if (!StringUtils.hasText(targetRef)) {
			parserContext.getReaderContext().error("The 'channel' attribute is required.", element);
		}
		builder.addConstructorArgReference(targetRef);
		String selectorRef = element.getAttribute("selector");
		String selectorExpression = element.getAttribute("selector-expression");
		if (StringUtils.hasText(selectorRef) && StringUtils.hasText(selectorExpression)) {
			parserContext.getReaderContext().error("Only one of 'selector' or 'selector-expression' is allowed", source);
		}
		if (StringUtils.hasText(selectorRef)) {
			builder.addConstructorArgReference(selectorRef);
		}
		else if (StringUtils.hasText(selectorExpression)) {
			BeanDefinitionBuilder expressionBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(ExpressionFactoryBean.class);
			expressionBuilder.addConstructorArgValue(selectorExpression);
			BeanDefinitionBuilder eemsBuilder = BeanDefinitionBuilder.genericBeanDefinition(ExpressionEvaluatingSelector.class);
			eemsBuilder.addConstructorArgValue(expressionBuilder.getBeanDefinition());
			builder.addConstructorArgValue(eemsBuilder.getBeanDefinition());
		}
		String timeout = element.getAttribute("timeout");
		if (StringUtils.hasText(timeout)) {
			builder.addPropertyValue("timeout", Long.parseLong(timeout));
		}
		String id = element.getAttribute("id");
		if (StringUtils.hasText(id)) {
			BeanDefinitionReaderUtils.registerBeanDefinition(new BeanDefinitionHolder(builder.getBeanDefinition(), id),
					parserContext.getRegistry());
			return id;
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(),
				parserContext.getRegistry());
	}

}
