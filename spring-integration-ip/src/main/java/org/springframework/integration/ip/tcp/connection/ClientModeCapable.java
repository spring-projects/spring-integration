/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedOperation;

/**
 * Edpoints implementing this interface are capable
 * of running in client-mode. For inbound endpoints,
 * this means that the endpoint establishes the connection
 * and then receives incoming data.
 * <p>
 * For an outbound adapter, it means that the adapter
 * will establish the connection rather than waiting
 * for a message to cause the connection to be
 * established.
 *
 * @author Gary Russell
 * @since 2.1
 *
 */
public interface ClientModeCapable {

	/**
	 * @return true if the endpoint is running in
	 * client mode.
	 */
	@ManagedAttribute
	boolean isClientMode();

	/**
	 * @return true if the endpoint is running in
	 * client mode.
	 */
	@ManagedAttribute
	boolean isClientModeConnected();

	/**
	 * Immediately attempt to establish the connection.
	 */
	@ManagedOperation
	void retryConnection();

}
