/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.store;

/**
 * A marker interface that indicates this message store has optimizations for
 * use in a {@link org.springframework.integration.channel.QueueChannel}.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public interface ChannelMessageStore extends BasicMessageGroupStore {

}
