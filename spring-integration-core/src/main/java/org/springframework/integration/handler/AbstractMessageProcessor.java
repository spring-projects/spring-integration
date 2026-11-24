/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.handler;

import org.jspecify.annotations.Nullable;

import org.springframework.integration.util.AbstractExpressionEvaluator;
import org.springframework.messaging.Message;

/**
 * @param <T> the expected payload type.
 *
 * @author Mark Fisher
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractMessageProcessor<T> extends AbstractExpressionEvaluator implements MessageProcessor<T> {

	@Nullable
	public abstract T processMessage(Message<?> message);

}
