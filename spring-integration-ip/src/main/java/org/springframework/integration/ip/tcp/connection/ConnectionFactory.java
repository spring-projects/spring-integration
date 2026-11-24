/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
