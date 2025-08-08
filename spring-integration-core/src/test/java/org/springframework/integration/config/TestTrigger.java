/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.config;

import java.time.Instant;

import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.TriggerContext;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
public class TestTrigger implements Trigger {

	@Override
	public Instant nextExecution(TriggerContext triggerContext) {
		throw new UnsupportedOperationException();
	}

}
