/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

/**
 * Inbound Gateway using a server connection factory - threading is controlled by the
 * factory. For java.net connections, each socket can process only one message at a time.
 * For java.nio connections, messages may be multiplexed but the client will need to
 * provide correlation logic. If the client is a {@link TcpOutboundGateway} multiplexing
 * is not used, but multiple concurrent connections can be used if the connection factory uses
 * single-use connections. For true asynchronous bidirectional communication, a pair of
 * inbound / outbound channel adapters should be used.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.inbound.TcpInboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpInboundGateway extends org.springframework.integration.ip.tcp.inbound.TcpInboundGateway {

}
