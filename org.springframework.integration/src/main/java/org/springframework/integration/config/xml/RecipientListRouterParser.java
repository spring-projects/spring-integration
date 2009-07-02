/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;recipient-list-router/&gt; element.
 * 
 * @author Oleg Zhurakousky
 * @since 1.0.3
 */
public class RecipientListRouterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder recipientListRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".router.RecipientListRouter");
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "recipient");
		Assert.notEmpty(childElements,
				"Recipient channel(s) must be defined (e.g., <recipient channel=\"channel1\"/>)");
	
		ManagedList channelList = new ManagedList();
		for (Element childElement : childElements) {
			channelList.add(new RuntimeBeanReference(childElement.getAttribute("channel")));
		}
		recipientListRouterBuilder.addPropertyValue("channels", channelList);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "ignore-send-failures");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(recipientListRouterBuilder, element, "apply-sequence");
		return recipientListRouterBuilder;
	}

}
