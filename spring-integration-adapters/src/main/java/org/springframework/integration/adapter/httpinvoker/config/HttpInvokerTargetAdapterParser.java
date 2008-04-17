/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter.httpinvoker.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.adapter.httpinvoker.HttpInvokerTargetAdapter;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;httpinvoker-target/&gt; element. 
 * 
 * @author Mark Fisher
 */
public class HttpInvokerTargetAdapterParser extends AbstractSingleBeanDefinitionParser {

	protected Class<?> getBeanClass(Element element) {
		return HandlerEndpoint.class;
	}

	protected boolean shouldGenerateId() {
		return false;
	}

	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		RootBeanDefinition adapterDef = new RootBeanDefinition(HttpInvokerTargetAdapter.class);
		String channel = element.getAttribute("channel");
		String url = element.getAttribute("url");
		if (!StringUtils.hasText(channel)) {
			throw new ConfigurationException("The 'channel' attribute is required.");
		}
		if (!StringUtils.hasText(url)) {
			throw new ConfigurationException("The 'url' attribute is required.");
		}
		adapterDef.getConstructorArgumentValues().addGenericArgumentValue(url);
		String adapterBeanName = parserContext.getReaderContext().generateBeanName(adapterDef);
		parserContext.registerBeanComponent(new BeanComponentDefinition(adapterDef, adapterBeanName));
		builder.addConstructorArgReference(adapterBeanName);
		Subscription subscription = new Subscription(channel);
		builder.addPropertyValue("subscription", subscription);
	}

}
