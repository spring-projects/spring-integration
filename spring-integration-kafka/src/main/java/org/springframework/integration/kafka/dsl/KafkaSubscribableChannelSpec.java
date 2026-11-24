/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.kafka.channel.SubscribableKafkaChannel;

/**
 * Spec for a subscribable channel.
 *
 * @param <C> the channel type.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public abstract class KafkaSubscribableChannelSpec<C extends SubscribableKafkaChannel>
		extends AbstractKafkaChannelSpec<KafkaSubscribableChannelSpec<C>, C> {

}
