/*
 * Copyright 2013-2024 the original author or authors.
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

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.DefaultMBeanObjectConverter;
import org.springframework.integration.jmx.MBeanTreePollingMessageSource;
import org.springframework.util.StringUtils;

/**
 * @author Stuart Williams
 * @author Gary Russell
 * @since 3.0
 *
 */
public class MBeanTreePollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(
				MBeanTreePollingMessageSource.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "server", "server");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "query-name-ref", "queryNameReference");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "query-expression-ref", "queryExpressionReference");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query-name", "queryName");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query-expression", "queryExpression");

		if (StringUtils.hasText(element.getAttribute("query-name"))
				&& StringUtils.hasText(element.getAttribute("query-name-ref"))) {
			parserContext.getReaderContext().error("Cannot have both `query-name' and 'query-name-ref'", element);
		}
		if (StringUtils.hasText(element.getAttribute("query-expression"))
				&& StringUtils.hasText(element.getAttribute("query-expression-ref"))) {
			parserContext.getReaderContext().error("Cannot have both `query-expression' and 'query-expression-ref'", element);
		}
		BeanComponentDefinition innerBeanDef = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String beanName = element.getAttribute("converter");

		if (innerBeanDef != null) {
			if (StringUtils.hasText(beanName)) {
				parserContext.getReaderContext().error("Cannot have both a 'converter' and an inner bean", element);
			}
			beanName = BeanDefinitionReaderUtils.generateBeanName(innerBeanDef.getBeanDefinition(), parserContext.getRegistry(), true);
			parserContext.getRegistry().registerBeanDefinition(beanName, innerBeanDef.getBeanDefinition());
		}
		else if (!StringUtils.hasText(beanName)) {
			BeanDefinitionBuilder childBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMBeanObjectConverter.class);
			beanName = BeanDefinitionReaderUtils.generateBeanName(childBuilder.getBeanDefinition(), parserContext.getRegistry(), true);
			parserContext.getRegistry().registerBeanDefinition(beanName, childBuilder.getBeanDefinition());
		}

		builder.addConstructorArgReference(beanName);

		return builder.getBeanDefinition();
	}

}
