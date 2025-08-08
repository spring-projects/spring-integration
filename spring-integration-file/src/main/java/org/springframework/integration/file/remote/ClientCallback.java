/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.file.remote;

/**
 * {@code RemoteFileTemplate} callback with the underlying client instance providing
 * access to lower level methods.
 *
 * @param <C> The type of the underlying client object.
 * @param <T> The return type of the callback method.
 *
 * @author Gary Russell
 *
 * @since 4.1
 */
@FunctionalInterface
public interface ClientCallback<C, T> {

	/**
	 * Called within the context of a
	 * {@link org.springframework.integration.file.remote.session.Session}. Perform some
	 * operation(s) on the client instance underlying the session. The caller will take
	 * care of closing the session after this method exits. However, the implementation is
	 * required to perform any clean up required by the client after performing
	 * operations.
	 * @param client The client instance.
	 * @return The return value.
	 */
	T doWithClient(C client);

}
