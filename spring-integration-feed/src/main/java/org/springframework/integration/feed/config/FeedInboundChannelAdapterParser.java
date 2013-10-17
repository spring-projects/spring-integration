/*
 * Copyright 2002-2013 the original author or authors.
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

package org.springframework.integration.feed.config;

import org.w3c.dom.Element;
import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Handles parsing the configuration for the feed inbound-channel-adapter.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @since 2.0
 */
public class FeedInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(final Element element, final ParserContext parserContext) {
		BeanDefinitionBuilder sourceBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.feed.inbound.FeedEntryMessageSource");
		sourceBuilder.addConstructorArgValue(element.getAttribute("url"));
		String feedFetcherRef = element.getAttribute("feed-fetcher");
		if (StringUtils.hasText(feedFetcherRef)) {
			sourceBuilder.addConstructorArgReference(feedFetcherRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sourceBuilder, element, "metadata-store");

		final String channelAdapterId = this.resolveId(element, sourceBuilder.getRawBeanDefinition(), parserContext);
		final String sourceBeanName = channelAdapterId + ".source";

		parserContext.getRegistry().registerBeanDefinition(sourceBeanName, sourceBuilder.getBeanDefinition());

		return new RuntimeBeanReference(sourceBeanName);

	}

}
