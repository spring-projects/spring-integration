/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.remote;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.session.Session;

/**
 * Callback invoked by {@code RemoteFileOperations.execute()} - allows multiple operations
 * on a session.
 *
 * @param <F> the type the operations accepts.
 * @param <T> the type the callback returns.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@FunctionalInterface
public interface SessionCallback<F, T> {

	/**
	 * Called within the context of a session.
	 * Perform some operation(s) on the session. The caller will take
	 * care of closing the session after this method exits.
	 * @param session The session.
	 * @return The result of type T.
	 * @throws IOException Any IOException.
	 */
	@Nullable T doInSession(Session<F> session) throws IOException;

}
