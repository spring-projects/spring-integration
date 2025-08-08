/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
