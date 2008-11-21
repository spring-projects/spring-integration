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
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;chain&gt; element.
 * 
 * @author Mark Fisher
 */
public class ChainParser extends AbstractConsumerEndpointParser {

	@Override
	@SuppressWarnings("unchecked")
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageHandlerChain.class);
		ManagedList handlerList = new ManagedList();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String childBeanName = this.parseChild((Element) child, parserContext);
				handlerList.add(new RuntimeBeanReference(childBeanName));
			}
		}
		builder.addPropertyValue("handlers", handlerList);
		return builder;
	}

	private String parseChild(Element element, ParserContext parserContext) {
		NamespaceHandler handler = parserContext.getReaderContext().getNamespaceHandlerResolver().resolve(element.getNamespaceURI());
		BeanDefinition beanDefinition = handler.parse(element, parserContext);
		Assert.notNull(beanDefinition, "BeanDefinition must not be null");
		Assert.isInstanceOf(AbstractBeanDefinition.class, beanDefinition);
		return BeanDefinitionReaderUtils.registerWithGeneratedName(
				(AbstractBeanDefinition) beanDefinition, parserContext.getRegistry());
	}

}
