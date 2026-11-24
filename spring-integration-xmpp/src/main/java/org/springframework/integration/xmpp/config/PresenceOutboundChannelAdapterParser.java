/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
