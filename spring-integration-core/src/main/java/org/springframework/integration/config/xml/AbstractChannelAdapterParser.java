/*
 * Copyright 2002-present the original author or authors.
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

import org.jspecify.annotations.Nullable;
import org.w3c.dom.Element;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Base parser for Channel Adapters.
 * <p>
 * Includes logic to determine {@link org.springframework.messaging.MessageChannel}:
 * if 'channel' attribute is defined - uses its value as 'channelName';
 * if 'id' attribute is defined - creates
 * {@link org.springframework.integration.channel.DirectChannel}
 * at runtime and uses id's value as 'channelName';
 * if current component is defined as nested element inside any other components e.g. &lt;chain&gt;
 * 'id' and 'channel' attributes will be ignored and this component will not be parsed as
 * {@link org.springframework.integration.endpoint.AbstractEndpoint}.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 */
public abstract class AbstractChannelAdapterParser extends AbstractBeanDefinitionParser {

	@Override
	protected final String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {

		String id = element.getAttribute(ID_ATTRIBUTE);
		if (!element.hasAttribute("channel")) {
			// the created channel will get the 'id', so the adapter's bean name includes a suffix
			id = id + ".adapter";
		}
		else if (!StringUtils.hasText(id)) {
			id = BeanDefinitionReaderUtils.generateBeanName(definition, parserContext.getRegistry(),
					parserContext.isNested());
		}
		return id;
	}

	@Override
	@SuppressWarnings("NullAway")
	protected final AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		String channelName = element.getAttribute("channel");
		if (!StringUtils.hasText(channelName)) {
			channelName = this.createDirectChannel(element, parserContext);
		}
		AbstractBeanDefinition beanDefinition = doParse(element, parserContext, channelName);
		MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
		if (!parserContext.isNested()) {
			String autoStartup = element.getAttribute(IntegrationNamespaceUtils.AUTO_STARTUP);
			if (StringUtils.hasText(autoStartup)) {
				propertyValues.add("autoStartup", new TypedStringValue(autoStartup));
			}
			String phase = element.getAttribute(IntegrationNamespaceUtils.PHASE);
			if (StringUtils.hasText(phase)) {
				propertyValues.add("phase", new TypedStringValue(phase));
			}
			String role = element.getAttribute(IntegrationNamespaceUtils.ROLE);
			if (StringUtils.hasText(role)) {
				propertyValues.add("role", new TypedStringValue(role));
			}
		}
		beanDefinition.setResource(parserContext.getReaderContext().getResource());
		beanDefinition.setSource(IntegrationNamespaceUtils.createElementDescription(element));
		return beanDefinition;
	}

	private @Nullable String createDirectChannel(Element element, ParserContext parserContext) {
		if (parserContext.isNested()) {
			return null;
		}
		return IntegrationNamespaceUtils.createDirectChannel(element, parserContext);
	}

	/**
	 * Subclasses must implement this method to parse the adapter element.
	 * The name of the MessageChannel bean is provided.
	 * @param element The element.
	 * @param parserContext The parser context.
	 * @param channelName The channel name.
	 * @return The bean definition.
	 */
	protected abstract AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName);

}
