/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * This class parses the schema for XMPP support.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XmppNamespaceHandler extends NamespaceHandlerSupport {

	public static final String XMPP_CONNECTION_BEAN_NAME = "xmppConnection";

	@Override
	public void init() {
		// connection
		registerBeanDefinitionParser("xmpp-connection", new XmppConnectionParser());

		// send/receive messages
		registerBeanDefinitionParser("inbound-channel-adapter", new ChatMessageInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new ChatMessageOutboundChannelAdapterParser());

		// presence
		registerBeanDefinitionParser("presence-inbound-channel-adapter", new PresenceInboundChannelAdapterParser());
		registerBeanDefinitionParser("presence-outbound-channel-adapter", new PresenceOutboundChannelAdapterParser());

		registerBeanDefinitionParser("header-enricher", new XmppHeaderEnricherParser());
	}

}
