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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;router/&gt; element.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class DefaultRouterParser extends AbstractDelegatingConsumerEndpointParser {

	private static final String CHANNEL_RESOLVER_PROPERTY = "channelResolver";


	@Override
	String getFactoryBeanClassName() {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".config.RouterFactoryBean";
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String resolverBeanName = element.getAttribute("channel-resolver");
		List<Element> mappingElements = DomUtils.getChildElementsByTagName(element, "mapping");
		if (!CollectionUtils.isEmpty(mappingElements)) {
			if (StringUtils.hasText(resolverBeanName)) {
				parserContext.getReaderContext().error(
						"The 'channel-resolver' attribute and 'mapping' sub-elements are mutually exclusive.",
						parserContext.extractSource(element));
			}
			BeanDefinitionBuilder channelResolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".support.channel.BeanFactoryChannelResolver");
			ManagedMap<String, String> channelMap = new ManagedMap<String, String>();
			for (Element mappingElement : mappingElements) {
				channelMap.put(mappingElement.getAttribute("value"), mappingElement.getAttribute("channel"));
			}
			builder.addPropertyValue("channelIdentifierMap", channelMap);
			builder.addPropertyValue(CHANNEL_RESOLVER_PROPERTY, channelResolverBuilder.getBeanDefinition());
		}
		else if (StringUtils.hasText(resolverBeanName)) {
			builder.addPropertyReference(CHANNEL_RESOLVER_PROPERTY, resolverBeanName);
		}
		else {
			RootBeanDefinition resolverBeanDefintion = new RootBeanDefinition(
					IntegrationNamespaceUtils.BASE_PACKAGE + ".support.channel.BeanFactoryChannelResolver");
			builder.addPropertyValue(CHANNEL_RESOLVER_PROPERTY, resolverBeanDefintion);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "resolution-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-send-failures");
	}

}
