/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
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
