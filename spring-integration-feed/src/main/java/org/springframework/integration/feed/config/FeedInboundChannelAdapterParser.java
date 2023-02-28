/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.feed.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.io.Resource;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.feed.inbound.FeedEntryMessageSource;
import org.springframework.util.StringUtils;

/**
 * Handles parsing the configuration for the feed inbound-channel-adapter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class FeedInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(final Element element, final ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(FeedEntryMessageSource.class);

		String url = element.getAttribute("url");
		boolean hasUrl = StringUtils.hasText(url);

		String resource = element.getAttribute("resource");
		boolean hasResource = StringUtils.hasText(resource);

		if (hasUrl == hasResource) {
			parserContext.getReaderContext().error("Exactly one of the 'url' or 'resource' is required.", element);
		}

		if (hasUrl) {
			sourceBuilder.addConstructorArgValue(url);
		}
		else {
			sourceBuilder.getBeanDefinition()
					.getConstructorArgumentValues()
					.addIndexedArgumentValue(0, resource, Resource.class.getName());
		}

		sourceBuilder.getBeanDefinition()
				.getConstructorArgumentValues()
				.addIndexedArgumentValue(1, element.getAttribute(ID_ATTRIBUTE));

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sourceBuilder, element, "metadata-store");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sourceBuilder, element, "feed-input", "syndFeedInput");

		IntegrationNamespaceUtils.setValueIfAttributeDefined(sourceBuilder, element, "preserve-wire-feed");

		return sourceBuilder.getBeanDefinition();
	}

}
