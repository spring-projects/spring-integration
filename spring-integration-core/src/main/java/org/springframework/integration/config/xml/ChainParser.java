/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.MessageHandlerChain;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;chain&gt; element.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 */
public class ChainParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MessageHandlerChain.class);
		String chainHandlerId = this.resolveId(element, builder.getRawBeanDefinition(), parserContext) + ".handler";
		ManagedList<BeanMetadataElement> handlerList = new ManagedList<BeanMetadataElement>();
		NodeList children = element.getChildNodes();
		int childOrder = 0;
		for (int i = 0; i < children.getLength(); i++) {
			Node child = children.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE && !"poller".equals(child.getLocalName())) {
				BeanMetadataElement childBeanMetadata = this.parseChild(chainHandlerId, (Element) child, childOrder++,
						parserContext, builder.getBeanDefinition());
				if ("gateway".equals(child.getLocalName())) {
					BeanDefinitionBuilder gwBuilder = BeanDefinitionBuilder.genericBeanDefinition(
							IntegrationNamespaceUtils.BASE_PACKAGE + ".gateway.RequestReplyMessageHandlerAdapter");
					gwBuilder.addConstructorArgValue(childBeanMetadata);
					handlerList.add(gwBuilder.getBeanDefinition());
				}
				else {
					handlerList.add(childBeanMetadata);
				}
			}
		}
		builder.addPropertyValue("handlers", handlerList);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		return builder;
	}

	private void validateChild(Element element, ParserContext parserContext) {

		final Object source = parserContext.extractSource(element);

		final String order = element.getAttribute(IntegrationNamespaceUtils.ORDER);

		if (StringUtils.hasText(order)) {
			parserContext.getReaderContext().error(IntegrationNamespaceUtils.createElementDescription(element) + " must not define " +
					"an 'order' attribute when used within a chain.", source);
		}

		final List<Element> pollerChildElements = DomUtils
				.getChildElementsByTagName(element, "poller");

		if (!pollerChildElements.isEmpty()) {
			parserContext.getReaderContext().error(IntegrationNamespaceUtils.createElementDescription(element) + " must not define " +
					"a 'poller' sub-element when used within a chain.", source);
		}

	}

	private BeanMetadataElement parseChild(String chainHandlerId, Element element, int order, ParserContext parserContext, BeanDefinition parentDefinition) {

		BeanDefinitionHolder holder = null;

		if ("bean".equals(element.getLocalName())) {
			holder = parserContext.getDelegate().parseBeanDefinitionElement(element, parentDefinition);
		}
		else {

			this.validateChild(element, parserContext);

			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(element, parentDefinition);
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("child BeanDefinition must not be null", element);
				return null;
			}
			else {
				String handlerBeanName = chainHandlerId + "$child" + order + "#" +
						BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
				String[] handlerAlias = IntegrationNamespaceUtils.generateAlias(element);
				holder = new BeanDefinitionHolder(beanDefinition, handlerBeanName, handlerAlias);
			}
		}
		if (StringUtils.hasText(element.getAttribute(ID_ATTRIBUTE))) {
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
			return new RuntimeBeanReference(holder.getBeanName());
		}

		return holder;
	}

}
