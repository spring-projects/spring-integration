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

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;

/**
 * Parser for the &lt;chain&gt; element.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class ChainParser extends AbstractConsumerEndpointParser {

	@Override
	@SuppressWarnings("unchecked")
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.MessageHandlerChain");
		ManagedList handlerList = new ManagedList();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && !"poller".equals(child.getLocalName())) {
				String childBeanName = this.parseChild((Element) child, parserContext, builder.getBeanDefinition());
				handlerList.add(new RuntimeBeanReference(childBeanName));
			}
		}
		builder.addPropertyValue("handlers", handlerList);
		return builder;
	}

	private String parseChild(Element element, ParserContext parserContext, BeanDefinition parentDefinition) {
		BeanDefinition beanDefinition;
		if (element.getLocalName().equals("bean")) {
			beanDefinition = parserContext.getDelegate().parseBeanDefinitionElement(element).getBeanDefinition();
		}
		else {
			beanDefinition = parserContext.getDelegate().parseCustomElement(element, parentDefinition);
		}
		if (beanDefinition == null) {
			parserContext.getReaderContext().error("child BeanDefinition must not be null", element);
		}
		String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
		BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, beanName);
		BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		return beanName;
	}

}
