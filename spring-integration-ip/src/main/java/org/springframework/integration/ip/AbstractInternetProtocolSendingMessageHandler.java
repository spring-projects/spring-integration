/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip;

/**
 * Base class for UDP MessageHandlers.
 *
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ip.udp.outbound.AbstractInternetProtocolSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public abstract class AbstractInternetProtocolSendingMessageHandler
		extends org.springframework.integration.ip.udp.outbound.AbstractInternetProtocolSendingMessageHandler {

	public AbstractInternetProtocolSendingMessageHandler(String host, int port) {
		super(host, port);
	}

}
