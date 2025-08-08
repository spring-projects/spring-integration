/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.support.management.metrics;

import java.util.concurrent.TimeUnit;

/**
 * @author Gary Russell
 * @since 5.0.4
 *
 */
public interface TimerFacade extends MeterFacade {

	void record(long time, TimeUnit unit);

}
