/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the MqttAdapter namespace.
 *
 * @author Gary Russell
 * @author Jaehong Park
 *
 * @since 4.0
 * @deprecated since 7.2 in favor of the {@code spring-integration-mqtt-client} module.
 */
@Deprecated(since = "7.2", forRemoval = true)
@SuppressWarnings("removal")
public class MqttNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("message-driven-channel-adapter", new MqttMessageDrivenChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new MqttOutboundChannelAdapterParser());
	}

}
