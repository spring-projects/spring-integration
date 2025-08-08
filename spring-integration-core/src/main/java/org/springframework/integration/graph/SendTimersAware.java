/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.graph;

import java.util.function.Supplier;

/**
 * @author Gary Russell
 * @since 5.2
 *
 */
public interface SendTimersAware {

	void sendTimers(Supplier<SendTimers> timers);

}
