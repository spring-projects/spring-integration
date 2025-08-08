/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.filter.ExpressionEvaluatingSelector;
import org.springframework.integration.router.RecipientListRouter;
import org.springframework.integration.router.RecipientListRouter.Recipient;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;recipient-list-router/&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class RecipientListRouterParser extends AbstractRouterParser {

	@Override
	protected BeanDefinition doParseRouter(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder recipientListRouterBuilder =
				BeanDefinitionBuilder.genericBeanDefinition(RecipientListRouter.class);
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "recipient");
		ManagedList<BeanDefinition> recipientList = new ManagedList<BeanDefinition>();
		for (Element childElement : childElements) {
			BeanDefinitionBuilder recipientBuilder = BeanDefinitionBuilder.genericBeanDefinition(Recipient.class);
			recipientBuilder.addConstructorArgReference(childElement.getAttribute("channel"));
			String expression = childElement.getAttribute("selector-expression");
			if (StringUtils.hasText(expression)) {
				BeanDefinition selectorDef = new RootBeanDefinition(ExpressionEvaluatingSelector.class);
				selectorDef.getConstructorArgumentValues().addGenericArgumentValue(expression);
				String selectorBeanName = parserContext.getReaderContext().registerWithGeneratedName(selectorDef);
				recipientBuilder.addConstructorArgReference(selectorBeanName);
			}
			recipientList.add(recipientBuilder.getBeanDefinition());
		}
		if (recipientList.size() > 0) {
			recipientListRouterBuilder.addPropertyValue("recipients", recipientList);
		}
		return recipientListRouterBuilder.getBeanDefinition();
	}

}
