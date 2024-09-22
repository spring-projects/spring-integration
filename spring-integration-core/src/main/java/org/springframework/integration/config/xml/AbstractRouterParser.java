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
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.RouterFactoryBean;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Base parser for routers.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Ngoc Nhan
 */
public abstract class AbstractRouterParser extends AbstractConsumerEndpointParser {

	@Override
	protected final BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RouterFactoryBean.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "default-output-channel");
		if (StringUtils.hasText(element.getAttribute("timeout"))
				&& StringUtils.hasText(element.getAttribute("send-timeout"))) {
			parserContext.getReaderContext().error("Only one of 'timeout' and 'send-timeout' is allowed", element);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "resolution-required");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ignore-send-failures");
		BeanDefinition targetRouterBeanDefinition = this.parseRouter(element, parserContext);
		builder.addPropertyValue("targetObject", targetRouterBeanDefinition);
		return builder;
	}

	protected final BeanDefinition parseRouter(Element element, ParserContext parserContext) {
		BeanDefinition beanDefinition = this.doParseRouter(element, parserContext);
		if (beanDefinition != null) {
			// check if mapping is provided otherwise returned values will be treated as channel names
			List<Element> mappingElements = DomUtils.getChildElementsByTagName(element, "mapping");
			if (!CollectionUtils.isEmpty(mappingElements)) {
				ManagedMap<String, String> channelMappings = new ManagedMap<>();
				for (Element mappingElement : mappingElements) {
					String key = mappingElement.getAttribute(this.getMappingKeyAttributeName());
					channelMappings.put(key, mappingElement.getAttribute("channel"));
				}
				beanDefinition.getPropertyValues().add("channelMappings", channelMappings);
			}
		}
		return beanDefinition;
	}

	/**
	 * Returns the name of the attribute that provides a key for the
	 * channel mappings. This can be overridden by subclasses.
	 *
	 * @return The mapping key attribute name.
	 */
	protected String getMappingKeyAttributeName() {
		return "value";
	}

	protected abstract BeanDefinition doParseRouter(Element element, ParserContext parserContext);

}
