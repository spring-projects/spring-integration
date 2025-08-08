/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.Socket;

import org.springframework.util.Assert;

/**
 * A client connection factory that creates {@link TcpNetConnection}s.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 */
public class TcpNetClientConnectionFactory extends
		AbstractClientConnectionFactory {

	private TcpSocketFactorySupport tcpSocketFactorySupport = new DefaultTcpNetSocketFactorySupport();

	private TcpNetConnectionSupport tcpNetConnectionSupport = new DefaultTcpNetConnectionSupport();

	/**
	 * Creates a TcpNetClientConnectionFactory for connections to the host and port.
	 * @param host the host
	 * @param port the port
	 */
	public TcpNetClientConnectionFactory(String host, int port) {
		super(host, port);
	}

	@Override
	protected TcpConnectionSupport buildNewConnection() {
		try {
			Socket socket = createSocket(getHost(), getPort());
			setSocketAttributes(socket);
			TcpConnectionSupport connection =
					this.tcpNetConnectionSupport.createNewConnection(socket, false, isLookupHost(),
							getApplicationEventPublisher(), getComponentName());
			TcpConnectionSupport wrapped = wrapConnection(connection);
			if (!wrapped.equals(connection)) {
				connection.setSenders(getSenders());
				connection = wrapped;
			}
			initializeConnection(connection, socket);
			this.getTaskExecutor().execute(connection);
			this.harvestClosedConnections();
			return connection;
		}
		catch (IOException e) {
			throw new UncheckedIOException(e);
		}
	}

	/**
	 * Set the {@link TcpNetConnectionSupport} to use to create connection objects.
	 * @param connectionSupport the connection support.
	 * @since 5.0
	 */
	public void setTcpNetConnectionSupport(TcpNetConnectionSupport connectionSupport) {
		Assert.notNull(connectionSupport, "'connectionSupport' cannot be null");
		this.tcpNetConnectionSupport = connectionSupport;
	}

	/**
	 * Set the {@link TcpSocketFactorySupport} used to create server sockets.
	 * @param tcpSocketFactorySupport the {@link TcpSocketFactorySupport}
	 */
	public void setTcpSocketFactorySupport(TcpSocketFactorySupport tcpSocketFactorySupport) {
		Assert.notNull(tcpSocketFactorySupport, "TcpSocketFactorySupport may not be null");
		this.tcpSocketFactorySupport = tcpSocketFactorySupport;
	}

	protected TcpSocketFactorySupport getTcpSocketFactorySupport() {
		return this.tcpSocketFactorySupport;
	}

	@Override
	public void start() {
		setActive(true);
		super.start();
	}

	/**
	 * Create a new {@link Socket}. This default implementation uses the default
	 * {@link javax.net.SocketFactory}. Override to use some other mechanism
	 * @param host The host.
	 * @param port The port.
	 * @return The Socket
	 * @throws IOException Any IOException.
	 */
	protected Socket createSocket(String host, int port) throws IOException {
		Socket socket = this.tcpSocketFactorySupport.getSocketFactory().createSocket();
		socket.connect(new InetSocketAddress(host, port), (int) getConnectTimeout().toMillis());
		return socket;
	}

}
