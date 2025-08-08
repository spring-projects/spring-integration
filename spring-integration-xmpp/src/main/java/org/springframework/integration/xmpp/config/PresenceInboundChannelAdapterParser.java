/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

/**
 * Parser for 'xmpp:presence-inbound-channel-adapter' element.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PresenceInboundChannelAdapterParser extends AbstractXmppInboundChannelAdapterParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.xmpp.inbound.PresenceListeningEndpoint";
	}

}
