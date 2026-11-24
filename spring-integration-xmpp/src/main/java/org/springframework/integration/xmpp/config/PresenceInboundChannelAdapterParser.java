/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
