/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.xml.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.router.MultiChannelRouter;
import org.springframework.integration.router.SingleChannelRouter;
import org.springframework.integration.xml.router.XPathMultiChannelNameResolver;
import org.springframework.integration.xml.router.XPathSingleChannelNameResolver;
import org.springframework.util.StringUtils;
import org.springframework.xml.xpath.XPathExpression;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Element;

/**
 * 
 * @author Jonas Partner
 * 
 */
public class XPathRouterParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {

		boolean multiChannel = Boolean.parseBoolean(element.getAttribute("multi-channel"));
		boolean resolutionRequired = Boolean.parseBoolean(element.getAttribute("resolution-required"));
		String xPathExpression = element.getAttribute("xpath-expression");
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		if ((StringUtils.hasText(xPathExpression) && StringUtils.hasText(xPathExpressionRef))
				|| (!StringUtils.hasText(xPathExpression) && !StringUtils.hasText(xPathExpressionRef))) {
			throw new ConfigurationException("Exactl one of xpath-expression or xpath-expression-ref is required");
		}

		BeanDefinitionBuilder resolverDefinitionBuilder = null;
		if (multiChannel) {
			builder.getBeanDefinition().setBeanClass(MultiChannelRouter.class);
			resolverDefinitionBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(XPathMultiChannelNameResolver.class);
		}
		else {
			builder.getBeanDefinition().setBeanClass(SingleChannelRouter.class);
			resolverDefinitionBuilder = BeanDefinitionBuilder
					.genericBeanDefinition(XPathSingleChannelNameResolver.class);
		}

		if (StringUtils.hasText(xPathExpression)) {
			XPathExpression expression = XPathExpressionFactory.createXPathExpression(xPathExpression);
			resolverDefinitionBuilder.addConstructorArgValue(expression);
		}
		else {
			resolverDefinitionBuilder.addConstructorArgReference(xPathExpressionRef);
		}

		builder.getBeanDefinition().getPropertyValues().addPropertyValue("resolutionRequired", resolutionRequired);
		builder.getBeanDefinition().getPropertyValues().addPropertyValue("channelNameResolver",
				resolverDefinitionBuilder.getBeanDefinition());
	}

}
