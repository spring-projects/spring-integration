/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
import org.springframework.integration.filter.MethodInvokingSelector;
import org.springframework.integration.selector.MessageSelectorChain;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;selector-chain/&gt; element.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SelectorChainParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return MessageSelectorChain.class;
	}

	@Override
	public void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		if (!StringUtils.hasText(element.getAttribute("id"))) {
			parserContext.getReaderContext().error("id is required", element);
		}
		parseSelectorChain(builder, element, parserContext);
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
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
					String method = ((Element) child).getAttribute("method");
					if (!StringUtils.hasText(method)) {
						selectors.add(new RuntimeBeanReference(ref));
					}
					else {
						selectors.add(buildMethodInvokingSelector(parserContext, ref, method));
					}
				}
				else if ("selector-chain".equals(nodeName)) {
					selectors.add(buildSelectorChain(parserContext, child));
				}
			}
		}
		builder.addPropertyValue("selectors", selectors);
	}

	private RuntimeBeanReference buildSelectorChain(ParserContext parserContext, Node child) {
		BeanDefinitionBuilder nestedBuilder = BeanDefinitionBuilder.genericBeanDefinition(MessageSelectorChain.class);
		this.parseSelectorChain(nestedBuilder, (Element) child, parserContext);
		String nestedBeanName = BeanDefinitionReaderUtils.registerWithGeneratedName(nestedBuilder.getBeanDefinition(),
				parserContext.getRegistry());
		return new RuntimeBeanReference(nestedBeanName);
	}

	private RuntimeBeanReference buildMethodInvokingSelector(ParserContext parserContext, String ref, String method) {
		BeanDefinitionBuilder methodInvokingSelectorBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingSelector.class);
		methodInvokingSelectorBuilder.addConstructorArgValue(new RuntimeBeanReference(ref));
		methodInvokingSelectorBuilder.addConstructorArgValue(method);
		return new RuntimeBeanReference(BeanDefinitionReaderUtils.registerWithGeneratedName(
				methodInvokingSelectorBuilder.getBeanDefinition(), parserContext.getRegistry()));
	}

}
