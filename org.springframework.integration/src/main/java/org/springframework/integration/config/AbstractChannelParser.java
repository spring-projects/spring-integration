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

package org.springframework.integration.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.interceptor.MessageSelectingInterceptor;
import org.springframework.integration.message.selector.PayloadTypeSelector;
import org.springframework.util.StringUtils;

/**
 * Base class for channel parsers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractChannelParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected abstract Class<?> getBeanClass(Element element);

	protected void postProcess(BeanDefinitionBuilder builder, Element element) {
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedList interceptors = null;
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && child.getLocalName().equals("interceptors")) {
				ChannelInterceptorParser interceptorParser = new ChannelInterceptorParser();
				interceptors = interceptorParser.parseInterceptors((Element) child, parserContext);
			}
		}
		if (interceptors == null) {
			interceptors = new ManagedList();
		}
		String datatypeAttr = element.getAttribute("datatype");
		if (StringUtils.hasText(datatypeAttr)) {
			String[] datatypes = StringUtils.commaDelimitedListToStringArray(datatypeAttr);
			RootBeanDefinition selectorDef = new RootBeanDefinition(PayloadTypeSelector.class);
			selectorDef.getConstructorArgumentValues().addGenericArgumentValue(datatypes);
			String selectorBeanName = parserContext.getReaderContext().generateBeanName(selectorDef);
			BeanComponentDefinition selectorComponent = new BeanComponentDefinition(selectorDef, selectorBeanName);
			parserContext.registerBeanComponent(selectorComponent);
			RootBeanDefinition interceptorDef = new RootBeanDefinition(MessageSelectingInterceptor.class);
			interceptorDef.getConstructorArgumentValues().addGenericArgumentValue(new RuntimeBeanReference(selectorBeanName));
			String interceptorBeanName = parserContext.getReaderContext().generateBeanName(interceptorDef);
			BeanComponentDefinition interceptorComponent = new BeanComponentDefinition(interceptorDef, interceptorBeanName);
			parserContext.registerBeanComponent(interceptorComponent);
			interceptors.add(new RuntimeBeanReference(interceptorBeanName));
		}
		builder.addPropertyValue("interceptors", interceptors);
		this.postProcess(builder, element);
	}

}
