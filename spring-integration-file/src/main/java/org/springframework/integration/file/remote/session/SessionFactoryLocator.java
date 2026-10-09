/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.file.remote.session;

import org.jspecify.annotations.Nullable;

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
	@Nullable
	SessionFactory<F> getSessionFactory(Object key);

}
