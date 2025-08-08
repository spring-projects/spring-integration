/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Marius Bogoevici
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class IntervalTriggerParserTests {

	@Autowired
	PollerMetadata pollerWithFixedRateAttribute;

	@Autowired
	PollerMetadata pollerWithFixedDelayAttribute;

	@Test
	public void testFixedRateTrigger() {
		Trigger trigger = this.pollerWithFixedRateAttribute.getTrigger();
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
		assertThat(periodicTrigger.getPeriodDuration()).isEqualTo(Duration.ofMillis(36));
		assertThat(periodicTrigger.isFixedRate()).isTrue();
	}

	@Test
	public void testFixedDelayTrigger() {
		Trigger trigger = this.pollerWithFixedDelayAttribute.getTrigger();
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
		assertThat(periodicTrigger.getPeriodDuration()).isEqualTo(Duration.ofMillis(37));
		assertThat(periodicTrigger.isFixedRate()).isFalse();
	}

}
