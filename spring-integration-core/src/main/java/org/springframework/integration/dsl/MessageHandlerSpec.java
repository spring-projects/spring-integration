/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageHandler;

/**
 * An {@link IntegrationComponentSpec} for {@link MessageHandler}s.
 *
 * @param <S> the target {@link ConsumerEndpointSpec} implementation type.
 * @param <H> the target {@link MessageHandler} implementation type.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public abstract class MessageHandlerSpec<S extends MessageHandlerSpec<S, H>, H extends MessageHandler>
		extends IntegrationComponentSpec<S, H> {

}
