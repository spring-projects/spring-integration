/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp;

/**
 * Tcp outbound channel adapter using a TcpConnection to
 * send data - if the connection factory is a server
 * factory, the TcpListener owns the connections. If it is
 * a client factory, this object owns the connection.
 *
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor or {@link org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class TcpSendingMessageHandler extends org.springframework.integration.ip.tcp.outbound.TcpSendingMessageHandler {

}
