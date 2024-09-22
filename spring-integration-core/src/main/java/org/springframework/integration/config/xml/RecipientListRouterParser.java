/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Ngoc Nhan
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
		if (!recipientList.isEmpty()) {
			recipientListRouterBuilder.addPropertyValue("recipients", recipientList);
		}
		return recipientListRouterBuilder.getBeanDefinition();
	}

}
