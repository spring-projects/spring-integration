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

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.DefaultMessageHandler;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;handler/&gt; element.
 * 
 * @author Mark Fisher
 */
public class HandlerParser implements BeanDefinitionParser {

	private static final String HANDLER_CHAIN_ELEMENT = "handler-chain";

	private static final String HANDLER_ELEMENT = "handler";

	private static final String HANDLERS_PROPERTY = "handlers";

	private static final String OBJECT_PROPERTY = "object";

	private static final String METHOD_NAME_PROPERTY = "methodName";


	public BeanDefinition parse(Element element, ParserContext parserContext) {
		if (HANDLER_CHAIN_ELEMENT.equals(element.getLocalName())) {
			return this.parseHandlerChain(element, parserContext);
		}
		else if (HANDLER_ELEMENT.equals(element.getLocalName())) {
			return this.parseHandler(element, parserContext, null);
		}
		return null;
	}

	private BeanDefinition parseHandlerChain(Element element, ParserContext parserContext) {
		RootBeanDefinition beanDefinition = new RootBeanDefinition(MessageHandlerChain.class);
		ManagedList handlers = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (HANDLER_ELEMENT.equals(localName)) {
					parseHandler((Element) child, parserContext, handlers);
				}
			}
		}
		beanDefinition.getPropertyValues().addPropertyValue(HANDLERS_PROPERTY, handlers);
		String id = element.getAttribute("id");
		String beanName = (StringUtils.hasText(id)) ? id : parserContext.getReaderContext().generateBeanName(beanDefinition);
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinition, beanName));
		return beanDefinition;
	}

	@SuppressWarnings("unchecked")
	private BeanDefinition parseHandler(Element element, ParserContext parserContext, ManagedList handlers) {
		boolean isInnerHandler = (handlers != null);
		String ref = element.getAttribute("ref");
		String method = element.getAttribute("method");
		String id = element.getAttribute("id");
		if (!isInnerHandler && (!StringUtils.hasText(id) || !StringUtils.hasText(ref) || !StringUtils.hasText(method))) {
			parserContext.getReaderContext().error("Top-level <handler> elements must provide 'id', 'ref', and 'method' attributes.",
					parserContext.extractSource(element));
		}
		if (isInnerHandler && StringUtils.hasText(id)) {
			parserContext.getReaderContext().error("The 'id' attribute is only supported for top-level <handler> elements.",
					parserContext.extractSource(element));			
		}
		if (StringUtils.hasText(method)) {
			BeanDefinitionHolder bdh = this.parseHandlerAdapter(id, ref, method, parserContext, isInnerHandler);
			if (handlers != null) {
				handlers.add(bdh.getBeanDefinition());
				return null;
			}
			return bdh.getBeanDefinition();
		}
		if (StringUtils.hasText(id)) {
			parserContext.getReaderContext().error("The 'id' attribute is only supported for handler adapters (when 'method' is also provided).",
					parserContext.extractSource(element));			
		}
		if (handlers != null) {
			handlers.add(new RuntimeBeanReference(ref));
		}
		return null;
	}

	private BeanDefinitionHolder parseHandlerAdapter(String id, String handlerRef, String handlerMethod, ParserContext parserContext, boolean isInnerHandler) {
		BeanDefinition handlerDef = new RootBeanDefinition(DefaultMessageHandler.class);
		handlerDef.getPropertyValues().addPropertyValue(OBJECT_PROPERTY, new RuntimeBeanReference(handlerRef));
		handlerDef.getPropertyValues().addPropertyValue(METHOD_NAME_PROPERTY, handlerMethod);
		String handlerBeanName = (StringUtils.hasText(id)) ? id :
				BeanDefinitionReaderUtils.generateBeanName(handlerDef, parserContext.getRegistry(), isInnerHandler);
		if (!isInnerHandler) {
			parserContext.registerBeanComponent(new BeanComponentDefinition(handlerDef, handlerBeanName));
		}
		return new BeanDefinitionHolder(handlerDef, handlerBeanName);
	}

}
