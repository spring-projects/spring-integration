/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.core;

/**
 * Generic (lambda) strategy interface for transformer.
 *
 * @param <S> the source type - 'transform from'.
 * @param <T> the target type - 'transform to'.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface GenericTransformer<S, T> {

	T transform(S source);

}
