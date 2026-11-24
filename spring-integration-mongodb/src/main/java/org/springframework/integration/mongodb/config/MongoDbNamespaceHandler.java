/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mongodb.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'mongodb' namespace.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 */
public class MongoDbNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new MongoDbInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new MongoDbOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new MongoDbOutboundGatewayParser());
	}

}
