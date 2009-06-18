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

import java.util.List;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.channel.MapBasedChannelResolver;
import org.springframework.integration.router.HeaderValueRouter;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;header-value-router/&gt; element.
 * 
 * @author Oleg Zhurakousky
 */
public class HeaderValueRouterParser extends RouterParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder headerValueRouterBuilder = BeanDefinitionBuilder.genericBeanDefinition(HeaderValueRouter.class);
		headerValueRouterBuilder.addConstructorArgValue(element.getAttribute("header-name"));
		
		BeanDefinitionBuilder mapBasedChannelResolverBuilder = null;
		// check if mapping is provided otherwise header values will be treated as channel names
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "mapping");
		if (childElements != null && childElements.size() > 0){
			mapBasedChannelResolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(MapBasedChannelResolver.class);
			ManagedMap channelMap = new ManagedMap();
			for (Element childElement : childElements) {
				channelMap.put(childElement.getAttribute("value"), new RuntimeBeanReference(childElement.getAttribute("channel")));
			}
			mapBasedChannelResolverBuilder.addPropertyValue("channelMap", channelMap);
		}
		if (mapBasedChannelResolverBuilder != null){
			headerValueRouterBuilder.addPropertyValue("channelResolver", mapBasedChannelResolverBuilder.getBeanDefinition());
		}
		
		BeanDefinitionBuilder rootBuilder = this.createBuilder();
		rootBuilder.addPropertyValue("targetObject", headerValueRouterBuilder.getBeanDefinition());
		return this.doParse(element, parserContext, rootBuilder);
	}

}
