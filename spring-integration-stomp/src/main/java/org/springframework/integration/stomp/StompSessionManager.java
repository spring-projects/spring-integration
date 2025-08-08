/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
