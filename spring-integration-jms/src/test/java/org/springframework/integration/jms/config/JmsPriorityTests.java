/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.0.2
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class JmsPriorityTests {

	@Autowired
	private MessageChannel outbound;

	@Autowired
	private PollableChannel results;

	@Before
	public void prepareActiveMq() {
		ActiveMqTestUtils.prepare();
	}

	@Test
	public void verifyPrioritySettingOnAdapterUsedAsJmsPriorityIfNoHeader() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").build();
		outbound.send(message);
		Message<?> result = results.receive(5000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof javax.jms.Message);
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertEquals(3, jmsMessage.getJMSPriority());
	}

	@Test
	public void verifyPriorityHeaderUsedAsJmsPriority() throws Exception {
		Message<?> message = MessageBuilder.withPayload("test").setPriority(7).build();
		outbound.send(message);
		Message<?> result = results.receive(5000);
		assertNotNull(result);
		assertTrue(result.getPayload() instanceof javax.jms.Message);
		javax.jms.Message jmsMessage = (javax.jms.Message) result.getPayload();
		assertEquals(7, jmsMessage.getJMSPriority());
	}

}
