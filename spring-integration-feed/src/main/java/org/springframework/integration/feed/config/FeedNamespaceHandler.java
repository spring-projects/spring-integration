/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.feed.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * NamespaceHandler for the feed module.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @since 2.0
 */
public class FeedNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FeedInboundChannelAdapterParser());
	}

}
