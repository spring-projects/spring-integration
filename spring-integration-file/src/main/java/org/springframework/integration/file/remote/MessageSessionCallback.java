/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.remote;

import java.io.IOException;

import org.springframework.integration.file.remote.session.Session;
import org.springframework.messaging.Message;

/**
 * Callback invoked by {@code RemoteFileOperations.executeForMessage()}
 * - allows multiple operations on a session.
 *
 * @param <F> the target protocol file type.
 * @param <T> the expected execution result.
 *
 * @author Artem Bilan
 *
 * @since 4.2
 */
@FunctionalInterface
public interface MessageSessionCallback<F, T> {

	/**
	 * Called within the context of a session and requestMessage.
	 * Perform some operation(s) on the session.
	 * The caller will take care of closing the session after this method exits.
	 *
	 * @param session The session.
	 * @param requestMessage The message to take in account with session operation(s).
	 * @return The result of type T.
	 * @throws IOException Any IOException.
	 */
	T doInSession(Session<F> session, Message<?> requestMessage) throws IOException;

}
