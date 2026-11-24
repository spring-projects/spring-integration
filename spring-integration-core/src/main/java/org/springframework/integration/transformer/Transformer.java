/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.transformer;

import org.springframework.integration.core.GenericTransformer;
import org.springframework.messaging.Message;

/**
 * Strategy interface for transforming a {@link Message}.
 *
 * @author Mark Fisher
 */
@FunctionalInterface
public interface Transformer extends GenericTransformer<Message<?>, Message<?>> {

	Message<?> transform(Message<?> message);

}
