/*
 * Copyright 2002-2023 the original author or authors.
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

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class InboundChannelAdapterWithDefaultPollerTests {

	@Autowired
	private SourcePollingChannelAdapter adapter;


	@Test
	public void verifyDefaultPollerInUse() {
		Trigger trigger = TestUtils.getPropertyValue(adapter, "trigger", Trigger.class);
		assertThat(trigger.getClass()).isEqualTo(PeriodicTrigger.class);
		DirectFieldAccessor triggerAccessor = new DirectFieldAccessor(trigger);
		assertThat(triggerAccessor.getPropertyValue("period")).isEqualTo(12345L);
		assertThat(triggerAccessor.getPropertyValue("fixedRate")).isEqualTo(Boolean.TRUE);
	}

}
