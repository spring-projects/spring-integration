/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

/**
 * A {@link SessionFactory} that implements this interface is capable of supporting a shared session.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public interface SharedSessionCapable {

	/**
	 * @return true if this factory uses a shared session.
	 */
	boolean isSharedSession();

	/**
	 * Resets the shared session so the next {@code #getSession()} will return a session
	 * using a new connection.
	 */
	void resetSharedSession();

}
