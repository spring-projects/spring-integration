/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
 */
@SpringJUnitConfig
public class InboundChannelAdapterWithDefaultPollerTests {

	@Autowired
	private SourcePollingChannelAdapter adapter;

	@Test
	public void verifyDefaultPollerInUse() {
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger).isInstanceOf(PeriodicTrigger.class);
		PeriodicTrigger periodicTrigger = (PeriodicTrigger) trigger;
		assertThat(periodicTrigger.getPeriodDuration()).isEqualTo(Duration.ofMillis(12345));
		assertThat(periodicTrigger.isFixedRate()).isTrue();
	}

}
