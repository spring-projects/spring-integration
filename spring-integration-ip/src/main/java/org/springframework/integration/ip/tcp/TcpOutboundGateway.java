/*
 * Copyright 2001-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

import org.springframework.integration.ip.tcp.connection.AbstractConnectionFactory;

/**
 * TCP outbound gateway that uses a client connection factory. If the factory is configured
 * for single-use connections, each request is sent on a new connection; if the factory does not use
 * single use connections, each request is blocked until the previous response is received
 * (or times out). Asynchronous requests/responses over the same connection are not
 * supported - use a pair of outbound/inbound adapters for that use case.
 * <p>
 * {@link org.springframework.context.Lifecycle} methods delegate to the underlying {@link AbstractConnectionFactory}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpOutboundGateway extends org.springframework.integration.ip.tcp.outbound.TcpOutboundGateway {

}
