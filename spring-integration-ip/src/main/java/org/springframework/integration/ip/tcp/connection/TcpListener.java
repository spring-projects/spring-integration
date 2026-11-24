/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.tcp.connection;

import org.springframework.messaging.Message;

/**
 * Classes that implement this interface may register with a
 * connection factory to receive messages retrieved from a
 * {@link TcpConnection}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@FunctionalInterface
public interface TcpListener {

	/**
	 * Called by a TCPConnection when a new message arrives.
	 * @param message The message.
	 */
	void onMessage(Message<?> message);

}
