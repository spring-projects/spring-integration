/*
 * Copyright 2002-present the original author or authors.
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
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 */
@SpringJUnitConfig
public class InboundChannelAdapterWithDefaultPollerTests {

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Test
	public void verifyDefaultPollerInUse() {
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger");
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
		assertThat(periodicTrigger.getPeriodDuration()).isEqualTo(Duration.ofMillis(12345));
		assertThat(periodicTrigger.isFixedRate()).isTrue();
	}

}
