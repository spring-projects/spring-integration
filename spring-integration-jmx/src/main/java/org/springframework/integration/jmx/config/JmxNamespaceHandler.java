/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>jmx</em> namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Dave Syer
 * @author Stuart Williams
 * @since 2.0
 */
public class JmxNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("operation-invoking-channel-adapter", new OperationInvokingChannelAdapterParser());
		this.registerBeanDefinitionParser("operation-invoking-outbound-gateway", new OperationInvokingOutboundGatewayParser());
		this.registerBeanDefinitionParser("attribute-polling-channel-adapter", new AttributePollingChannelAdapterParser());
		this.registerBeanDefinitionParser("tree-polling-channel-adapter", new MBeanTreePollingChannelAdapterParser());
		this.registerBeanDefinitionParser("notification-listening-channel-adapter", new NotificationListeningChannelAdapterParser());
		this.registerBeanDefinitionParser("notification-publishing-channel-adapter", new NotificationPublishingChannelAdapterParser());
		this.registerBeanDefinitionParser("mbean-export", new MBeanExporterParser());
	}

}
