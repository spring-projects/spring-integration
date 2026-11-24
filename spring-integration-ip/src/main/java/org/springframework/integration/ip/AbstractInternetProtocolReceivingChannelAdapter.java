/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip;

/**
 * Base class for inbound TCP/UDP Channel Adapters.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.ip.udp.inbound.AbstractInternetProtocolReceivingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public abstract class AbstractInternetProtocolReceivingChannelAdapter
		extends org.springframework.integration.ip.udp.inbound.AbstractInternetProtocolReceivingChannelAdapter {

	public AbstractInternetProtocolReceivingChannelAdapter(int port) {
		super(port);
	}

}
