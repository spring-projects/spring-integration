/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.groovy;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 *
 * @since 2.2
 */
@SpringJUnitConfig
@DirtiesContext
public class GroovyControlBusIntegrationTests {

	@Autowired
	private MessageChannel controlBus;

	@Autowired
	private PollableChannel output;

	@Autowired
	private MessageChannel delayerInput;

	@Autowired
	ThreadPoolTaskScheduler scheduler;

	@Test
	public void testDelayerManagement() throws IOException {
		Message<String> testMessage = MessageBuilder.withPayload("test").build();
		this.delayerInput.send(testMessage);
		this.delayerInput.send(testMessage);

		this.scheduler.destroy();
		// ensure the delayer did not release any messages
		assertThat(this.output.receive(500)).isNull();
		this.scheduler.afterPropertiesSet();

		Resource scriptResource = new ClassPathResource("GroovyControlBusDelayerManagementTest.groovy", getClass());
		ScriptSource scriptSource = new ResourceScriptSource(scriptResource);
		Message<?> message = MessageBuilder.withPayload(scriptSource.getScriptAsString()).build();
		this.controlBus.send(message);

		assertThat(this.output.receive(10000)).isNotNull();
		assertThat(this.output.receive(10000)).isNotNull();
	}

}
