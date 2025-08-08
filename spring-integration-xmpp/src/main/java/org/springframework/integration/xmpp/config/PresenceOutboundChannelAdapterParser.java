/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

/**
 * Parser for 'xmpp:presence-outbound-channel-adapter' element
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PresenceOutboundChannelAdapterParser extends AbstractXmppOutboundChannelAdapterParser {

	@Override
	protected String getHandlerClassName() {
		return "org.springframework.integration.xmpp.outbound.PresenceSendingMessageHandler";
	}

}
