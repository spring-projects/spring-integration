/*
 * Copyright 2002-2011 the original author or authors.
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
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser for routers.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRouterParser extends AbstractConsumerEndpointParser {

	@Override
	protected final BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".config.RouterFactoryBean");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "channel-resolution-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-send-failures");
		BeanDefinition targetRouterBeanDefinition = this.parseRouter(element, parserContext);
		builder.addPropertyValue("targetObject", targetRouterBeanDefinition);
		return builder;
	}

	protected final BeanDefinition parseRouter(Element element, ParserContext parserContext) {
		BeanDefinition beanDefinition = this.doParseRouter(element, parserContext);
		if (beanDefinition != null) {
			String channelResolver = element.getAttribute("channel-resolver");
			if (StringUtils.hasText(channelResolver)){
				beanDefinition.getPropertyValues().add("channelResolver", new RuntimeBeanReference(channelResolver));
			}
			// check if mapping is provided otherwise returned values will be treated as channel names
			List<Element> childElements = DomUtils.getChildElementsByTagName(element, "mapping");
			if (childElements != null && childElements.size() > 0) {
				ManagedMap<String, String> channelMap = new ManagedMap<String, String>();
				for (Element childElement : childElements) {
					String key = childElement.getAttribute(this.getMappingKeyAttributeValue());
					channelMap.put(key, childElement.getAttribute("channel"));
				}
				beanDefinition.getPropertyValues().add("channelIdentifierMap", channelMap);
			}
		}
		return beanDefinition;
	}

	protected abstract BeanDefinition doParseRouter(Element element, ParserContext parserContext);
	
	protected String getMappingKeyAttributeValue(){
		return "value";
	}

}
