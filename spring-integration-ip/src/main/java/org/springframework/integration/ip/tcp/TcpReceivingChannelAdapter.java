/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp;

/**
 * Tcp inbound channel adapter using a TcpConnection to
 * receive data - if the connection factory is a server
 * factory, this Listener owns the connections. If it is
 * a client factory, the sender owns the connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpReceivingChannelAdapter
		extends org.springframework.integration.ip.tcp.inbound.TcpReceivingChannelAdapter {

}
