/*
 * Copyright 2002-2010 the original author or authors.
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
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * Parser for the &lt;chain&gt; element.
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 */
public class ChainParser extends AbstractConsumerEndpointParser {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(IntegrationNamespaceUtils.BASE_PACKAGE + ".handler.MessageHandlerChain");
		ManagedList handlerList = new ManagedList();
		NodeList children = element.getChildNodes();
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && !"poller".equals(child.getLocalName())) {
				BeanDefinitionHolder holder = this.parseChild((Element) child, parserContext, builder.getBeanDefinition());
				if ("gateway".equals(child.getLocalName())){
					BeanDefinitionBuilder gwBuilder = BeanDefinitionBuilder.genericBeanDefinition(
							IntegrationNamespaceUtils.BASE_PACKAGE + ".gateway.RequestReplyMessageHandlerAdapter");
					gwBuilder.addConstructorArgValue(holder);
					handlerList.add(gwBuilder.getBeanDefinition());
				}
				else {
					handlerList.add(holder);
				}		
			}
		}
		builder.addPropertyValue("handlers", handlerList);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

	private BeanDefinitionHolder parseChild(Element element, ParserContext parserContext, BeanDefinition parentDefinition) {
		BeanDefinitionHolder holder = null;
		if (element.getLocalName().equals("bean")) {
			holder = parserContext.getDelegate().parseBeanDefinitionElement(element, parentDefinition);
		}
		else {
			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(element, parentDefinition);
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("child BeanDefinition must not be null", element);
			}
			else {
				String beanName = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry(), true);
				holder = new BeanDefinitionHolder(beanDefinition, beanName);
			}
		}
		return holder;
	}

}
