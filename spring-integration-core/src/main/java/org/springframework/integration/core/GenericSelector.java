/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.core;

/**
 * Generic (lambda) strategy interface for selector.
 *
 * @param <S> the source type to accept.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface GenericSelector<S> {

	boolean accept(S source);

}
