/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.file.remote;

import java.io.IOException;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.file.remote.session.Session;

/**
 * Simple convenience implementation of {@link SessionCallback} for cases where
 * no result is returned.
 *
 * @param <F> the target system file type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
@FunctionalInterface
public interface SessionCallbackWithoutResult<F> extends SessionCallback<F, Object> {

	@Override
	default @Nullable Object doInSession(Session<F> session) throws IOException {
		doInSessionWithoutResult(session);
		return null;
	}

	/**
	 * Called within the context of a session.
	 * Perform some operation(s) on the session. The caller will take
	 * care of closing the session after this method exits.
	 * @param session The session.
	 * @throws IOException Any IOException.
	 */
	void doInSessionWithoutResult(Session<F> session) throws IOException;

}
