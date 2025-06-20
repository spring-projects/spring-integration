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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class CronTriggerParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void checkConfigWithAttribute() {
		Object poller = context.getBean("pollerWithAttribute");
		assertThat(poller.getClass()).isEqualTo(PollerMetadata.class);
		PollerMetadata metadata = (PollerMetadata) poller;
		Trigger trigger = metadata.getTrigger();
		assertThat(trigger.getClass()).isEqualTo(CronTrigger.class);
		String expression = TestUtils.getPropertyValue(trigger, "expression.expression", String.class);
		assertThat(expression).isEqualTo("*/10 * 9-17 * * MON-FRI");
	}

}

