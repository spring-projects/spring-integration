/*
 * Copyright 2019-2023 the original author or authors.
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
