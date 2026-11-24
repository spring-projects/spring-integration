/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.mqtt.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the MqttAdapter namespace.
 *
 * @author Gary Russell
 *
 * @since 4.0
 *
 */
public class MqttNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("message-driven-channel-adapter", new MqttMessageDrivenChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new MqttOutboundChannelAdapterParser());
	}

}
