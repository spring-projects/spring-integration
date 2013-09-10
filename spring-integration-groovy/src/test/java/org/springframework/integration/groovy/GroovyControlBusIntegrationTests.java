/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scripting.ScriptSource;
import org.springframework.scripting.support.ResourceScriptSource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 2.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class GroovyControlBusIntegrationTests {

	@Autowired
	private MessageChannel controlBus;

	@Autowired
	private PollableChannel controlBusOutput;

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
		assertNull(this.output.receive(500));
		this.scheduler.afterPropertiesSet();

		Resource scriptResource = new ClassPathResource("GroovyControlBusDelayerManagementTest.groovy", this.getClass());
		ScriptSource scriptSource = new ResourceScriptSource(scriptResource);
		Message<?> message = MessageBuilder.withPayload(scriptSource.getScriptAsString()).build();
		this.controlBus.send(message);

		assertNotNull(this.output.receive(1000));
		assertNotNull(this.output.receive(1000));
	}
}
