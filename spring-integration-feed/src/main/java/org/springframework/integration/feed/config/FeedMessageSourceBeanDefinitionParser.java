package org.springframework.integration.feed.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.feed.FeedEntryReaderMessageSource;
import org.springframework.integration.feed.FeedReaderMessageSource;
import org.w3c.dom.Element;

/**
 * Handles parsing the configuration for the feed inbound channel adapter.
 *
 * @author Josh Long
 */
public class FeedMessageSourceBeanDefinitionParser extends AbstractPollingInboundChannelAdapterParser {


	private String packageName = FeedReaderMessageSource.class.getPackage().getName();


	@Override
	protected String parseSource(final Element element, final ParserContext parserContext) {
		String pftoe = (element.getAttribute("prefer-updated-feed-to-entries"));

        pftoe = pftoe == null ? "false" : pftoe.trim().toLowerCase();

		boolean preferFeed = pftoe.equalsIgnoreCase(Boolean.TRUE.toString().toLowerCase());
		String className = this.packageName + "." + (preferFeed ?
				FeedReaderMessageSource.class.getSimpleName() :
				FeedEntryReaderMessageSource.class.getSimpleName()
		);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(className);
		builder.addPropertyValue("feedUrl", element.getAttribute("feed"));

		if (!preferFeed) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "backlog-cache-size", "maximumBacklogCacheSize");
		}

		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}
}