/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.core;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.MessageHeaders;

/**
 * A functional interface to specify {@link org.springframework.messaging.MessageHandler}
 * logic with Java 8 Lambda expression:
 * <pre class="code">
 * {@code
 *  .<Integer>handle((p, h) -> p / 2)
 * }
 * </pre>
 *
 * @param <P> the expected {@code payload} type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
@FunctionalInterface
public interface GenericHandler<P> {

	@Nullable Object handle(P payload, MessageHeaders headers);

}
