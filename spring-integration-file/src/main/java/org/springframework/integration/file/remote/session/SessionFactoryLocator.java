/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

/**
 *
 * A factory returning a {@link SessionFactory} based on some key.
 *
 * @param <F> the target system file type.
 *
 * @author Gary Russell
 *
 * @since 4.2
 */
@FunctionalInterface
public interface SessionFactoryLocator<F> {

	/**
	 * Return a {@link SessionFactory} for the key.
	 * @param key the key.
	 * @return the session factory.
	 */
	SessionFactory<F> getSessionFactory(Object key);

}
