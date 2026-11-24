/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl.support;

import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * An "artificial" {@link MessageChannel} implementation which will be unwrapped to the
 * {@link MessageChannel} bean on the bean registration phase.
 * For internal use only.
 *
 * @param name the name of the target {@link MessageChannel} bean.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see org.springframework.integration.dsl.context.IntegrationFlowBeanPostProcessor
 */
public record MessageChannelReference(String name) implements MessageChannel {

	public MessageChannelReference {
		Assert.notNull(name, "'name' must not be null");
	}

	@Override
	public boolean send(Message<?> message) {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean send(Message<?> message, long timeout) {
		throw new UnsupportedOperationException();
	}

}
