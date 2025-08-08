/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.file.remote;

import java.io.IOException;

import org.springframework.integration.file.remote.session.Session;

/**
 * Callback invoked by {@code RemoteFileOperations.execute()} - allows multiple operations
 * on a session.
 *
 * @param <F> the type the operations accepts.
 * @param <T> the type the callback returns.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
@FunctionalInterface
public interface SessionCallback<F, T> {

	/**
	 * Called within the context of a session.
	 * Perform some operation(s) on the session. The caller will take
	 * care of closing the session after this method exits.
	 *
	 * @param session The session.
	 * @return The result of type T.
	 * @throws IOException Any IOException.
	 */
	T doInSession(Session<F> session) throws IOException;

}
