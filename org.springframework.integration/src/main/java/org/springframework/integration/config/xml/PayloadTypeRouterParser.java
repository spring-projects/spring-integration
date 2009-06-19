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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;payload-type-router/&gt; element.
 * 
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @since 1.0.3
 */
public class PayloadTypeRouterParser extends RouterParser {

	@Override
	@SuppressWarnings("unchecked")
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder payloadTypeRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".router.PayloadTypeRouter");
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "mapping");
		Assert.notEmpty(childElements,
				"Type mapping must be provided (e.g., <mapping type=\"X\" channel=\"channel1\"/>)");
		ManagedMap channelMap = new ManagedMap();
		for (Element childElement : childElements) {
			String typeName = childElement.getAttribute("type");
			ClassLoader classLoader = parserContext.getReaderContext().getBeanClassLoader();
			if (classLoader == null) {
				classLoader = ClassUtils.getDefaultClassLoader();
			}
			Assert.isTrue(ClassUtils.isPresent(typeName, classLoader), typeName + " can not be loaded");
			channelMap.put(typeName, new RuntimeBeanReference(childElement.getAttribute("channel")));
		}
		payloadTypeRouterBuilder.addPropertyValue("payloadTypeChannelMap", channelMap);
		BeanDefinitionBuilder rootBuilder = this.createBuilder();
		rootBuilder.addPropertyValue("targetObject", payloadTypeRouterBuilder.getBeanDefinition());
		return this.doParse(element, parserContext, rootBuilder);
	}
	
}
