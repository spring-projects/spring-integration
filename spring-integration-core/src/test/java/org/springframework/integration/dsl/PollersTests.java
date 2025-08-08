/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.dsl;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.scheduling.support.PeriodicTrigger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.1.4
 *
 */
public class PollersTests {

	@Test
	public void testDurations() {
		PeriodicTrigger trigger = (PeriodicTrigger) Pollers.fixedDelay(Duration.ofMinutes(1L)).getObject().getTrigger();
		assertThat(trigger.getPeriodDuration()).isEqualTo(Duration.ofSeconds(60));
		assertThat(trigger.isFixedRate()).isFalse();
		trigger = (PeriodicTrigger) Pollers.fixedDelay(Duration.ofMinutes(1L), Duration.ofSeconds(10L))
				.getObject().getTrigger();
		assertThat(trigger.getPeriodDuration()).isEqualTo(Duration.ofSeconds(60));
		assertThat(trigger.getInitialDelayDuration()).isEqualTo(Duration.ofSeconds(10));
		assertThat(trigger.isFixedRate()).isFalse();
		trigger = (PeriodicTrigger) Pollers.fixedRate(Duration.ofMinutes(1L)).getObject().getTrigger();
		assertThat(trigger.getPeriodDuration()).isEqualTo(Duration.ofSeconds(60));
		assertThat(trigger.isFixedRate()).isTrue();
		trigger = (PeriodicTrigger) Pollers.fixedRate(Duration.ofMinutes(1L), Duration.ofSeconds(10L))
				.getObject().getTrigger();
		assertThat(trigger.getPeriodDuration()).isEqualTo(Duration.ofSeconds(60));
		assertThat(trigger.getInitialDelayDuration()).isEqualTo(Duration.ofSeconds(10));
		assertThat(trigger.isFixedRate()).isTrue();
	}

}
