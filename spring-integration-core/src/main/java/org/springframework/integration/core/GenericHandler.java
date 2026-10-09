/*
 * Copyright 2016-present the original author or authors.
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
