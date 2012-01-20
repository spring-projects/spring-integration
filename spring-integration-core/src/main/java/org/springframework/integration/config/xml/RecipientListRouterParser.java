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

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;recipient-list-router/&gt; element.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class RecipientListRouterParser extends AbstractConsumerEndpointParser {

	@Override
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder recipientListRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".router.RecipientListRouter");
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "recipient");
		Assert.notEmpty(childElements,
				"At least one recipient channel must be defined (e.g., <recipient channel=\"channel1\"/>).");
		ManagedList recipientList = new ManagedList();
		for (Element childElement : childElements) {
			BeanDefinitionBuilder recipientBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.router.RecipientListRouter.Recipient");
			recipientBuilder.addConstructorArgReference(childElement.getAttribute("channel"));
			String expression = childElement.getAttribute("selector-expression");
			if (StringUtils.hasText(expression)) {
				BeanDefinition selectorDef = new RootBeanDefinition(
						"org.springframework.integration.filter.ExpressionEvaluatingSelector");
				selectorDef.getConstructorArgumentValues().addGenericArgumentValue(expression);
				String selectorBeanName = parserContext.getReaderContext().registerWithGeneratedName(selectorDef);
				recipientBuilder.addConstructorArgReference(selectorBeanName);
			}
			recipientList.add(recipientBuilder.getBeanDefinition());
		}
		recipientListRouterBuilder.addPropertyValue("recipients", recipientList);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "ignore-send-failures");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "apply-sequence");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(recipientListRouterBuilder, element, "default-output-channel");
		return recipientListRouterBuilder;
	}

}
