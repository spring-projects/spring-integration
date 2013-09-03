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
package org.springframework.integration.jmx.config;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.DefaultMBeanObjectConverter;
import org.w3c.dom.Element;

/**
 * @author Stuart Williams
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
				"org.springframework.integration.jmx.MBeanTreePollingMessageSource");

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "server", "server");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "query-name-ref", "queryNameReference");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "query-expression-ref", "queryExpressionReference");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query-name", "queryName");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query-expression", "queryExpression");

		BeanComponentDefinition innerBeanDef = IntegrationNamespaceUtils.parseInnerHandlerDefinition(element, parserContext);
		String beanName;

		if (innerBeanDef != null) {
			beanName = BeanDefinitionReaderUtils.generateBeanName(innerBeanDef.getBeanDefinition(), parserContext.getRegistry(), true);
			parserContext.getRegistry().registerBeanDefinition(beanName, innerBeanDef.getBeanDefinition());
		}
		else {
			BeanDefinitionBuilder childBuilder = BeanDefinitionBuilder.genericBeanDefinition(DefaultMBeanObjectConverter.class);
			beanName = BeanDefinitionReaderUtils.generateBeanName(childBuilder.getBeanDefinition(), parserContext.getRegistry(), true);
			parserContext.getRegistry().registerBeanDefinition(beanName, childBuilder.getBeanDefinition());
		}

		builder.addConstructorArgReference(beanName);

		return builder.getBeanDefinition();
	}

}
