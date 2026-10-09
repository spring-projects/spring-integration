/*
 * Copyright 2002-present the original author or authors.
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

