/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
