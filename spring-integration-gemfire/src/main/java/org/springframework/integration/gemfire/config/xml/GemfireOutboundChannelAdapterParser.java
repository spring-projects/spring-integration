/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.gemfire.config.xml;

import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.gemfire.outbound.CacheWritingMessageHandler;
import org.springframework.util.xml.DomUtils;

/**
 * @author David Turanski
 * @author Gary Russell
 * @since 2.1
 */
public class GemfireOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	private static final String CACHE_ENTRIES_PROPERTY = "cacheEntries";

	private static final String CACHE_ENTRIES_ELEMENT = "cache-entries";

	private static final String REGION_ATTRIBUTE = "region";

	/* (non-Javadoc)
	 * @see AbstractOutboundChannelAdapterParser#parseConsumer(Element, ParserContext)
	 */
	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder cacheWritingMessageHandler = BeanDefinitionBuilder.genericBeanDefinition(
				CacheWritingMessageHandler.class);
		if (!element.hasAttribute(REGION_ATTRIBUTE)) {
			parserContext.getReaderContext().error("'region' attribute is required.", element);
		}

		cacheWritingMessageHandler.addConstructorArgReference(element.getAttribute(REGION_ATTRIBUTE));

		Element cacheEntries = DomUtils.getChildElementByTagName(element, CACHE_ENTRIES_ELEMENT);
		if (cacheEntries != null) {
			Map<?, ?> map = parserContext.getDelegate()
					.parseMapElement(cacheEntries, cacheWritingMessageHandler.getBeanDefinition());
			cacheWritingMessageHandler.addPropertyValue(CACHE_ENTRIES_PROPERTY, map);
		}
		return cacheWritingMessageHandler.getBeanDefinition();
	}
}
