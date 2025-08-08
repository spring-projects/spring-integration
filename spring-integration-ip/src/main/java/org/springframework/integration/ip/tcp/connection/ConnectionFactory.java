/*
 * Copyright © 2001 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2001-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.integration.support.management.ManageableLifecycle;

/**
 * A factory used to create TcpConnection objects.
 *
 * @author Gary Russell
 * @since 2.0
 *
 */
public interface ConnectionFactory extends ManageableLifecycle {

	TcpConnection getConnection() throws InterruptedException;

}
