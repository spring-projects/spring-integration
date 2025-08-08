/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

import org.springframework.integration.store.MessageGroup;

/**
 * Strategy for determining when a group of messages reaches a state of
 * completion (i.e. can trip a barrier).
 *
 * @author Mark Fisher
 * @author Dave Syer
 */
@FunctionalInterface
public interface ReleaseStrategy {

	boolean canRelease(MessageGroup group);

}
