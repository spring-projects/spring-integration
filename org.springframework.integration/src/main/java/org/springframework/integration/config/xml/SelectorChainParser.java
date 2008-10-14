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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.util.Assert;

/**
 * Parser for the &lt;selector-chain/&gt; element.
 * 
 * @author Mark Fisher
 */
public class SelectorChainParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return MessageSelectorChain.class;
	}

	public void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		Assert.hasText(element.getAttribute("id"), "id is required");
		this.parseSelectorChain(builder, element, parserContext);
	}

	@SuppressWarnings("unchecked")
	private void parseSelectorChain(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "voting-strategy");
		ManagedList selectors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String nodeName = child.getLocalName();
				if ("selector".equals(nodeName)) {
					String ref = ((Element) child).getAttribute("ref");
					selectors.add(new RuntimeBeanReference(ref));
				}
				else if ("selector-chain".equals(nodeName)) {
					BeanDefinitionBuilder nestedBuilder =
							BeanDefinitionBuilder.genericBeanDefinition(MessageSelectorChain.class);
					this.parseSelectorChain(nestedBuilder, (Element) child, parserContext);
					String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(
							nestedBuilder.getBeanDefinition(), parserContext.getRegistry());
					selectors.add(new RuntimeBeanReference(nestedBeanName));
				}
			}
		}
		builder.addPropertyValue("selectors", selectors);
	}

}
