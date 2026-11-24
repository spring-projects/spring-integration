/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.stomp;

import org.springframework.messaging.simp.stomp.StompSessionHandler;

/**
 * An abstraction to manage the STOMP Session and connection/disconnection
 * for {@link StompSessionHandler}.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public interface StompSessionManager {

	void connect(StompSessionHandler handler);

	void disconnect(StompSessionHandler handler);

	boolean isAutoReceiptEnabled();

	boolean isConnected();

}
