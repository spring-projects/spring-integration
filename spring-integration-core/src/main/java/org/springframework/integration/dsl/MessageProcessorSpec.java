/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.handler.MessageProcessor;

/**
 * The {@link IntegrationComponentSpec} specific base class
 * for {@link MessageProcessor}s.
 *
 * @param <S> the target {@link MessageProcessorSpec} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageProcessorSpec<S extends MessageProcessorSpec<S>>
		extends IntegrationComponentSpec<S, MessageProcessor<?>> {

}
