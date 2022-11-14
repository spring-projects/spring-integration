/*
 * Copyright 2002-2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
