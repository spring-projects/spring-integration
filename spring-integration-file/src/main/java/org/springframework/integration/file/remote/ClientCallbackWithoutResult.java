/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.file.remote;

/**
 * {@code RemoteFileTemplate} callback with the underlying client instance providing
 * access to lower level methods where no result is returned.
 *
 * @param <C> The type of the underlying client object.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.1
 */
@FunctionalInterface
public interface ClientCallbackWithoutResult<C> extends ClientCallback<C, Object> {

	@Override
	default Object doWithClient(C client) {
		doWithClientWithoutResult(client);
		return null;
	}

	/**
	 * Called within the context of a session.
	 * Perform some operation(s) on the client instance underlying the session. The caller will take
	 * care of closing the session after this method exits. However, the implementation
	 * is required to perform any clean up required by the client after performing
	 * operations.
	 * @param client The client.
	 */
	void doWithClientWithoutResult(C client);

}
