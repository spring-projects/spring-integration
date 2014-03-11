/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.jms;

import static org.junit.Assert.assertNotNull;

import javax.jms.ConnectionFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.jms.JmsInboundChannelAdapterTests.CFConfig;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration(classes=CFConfig.class)
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class JmsInboundChannelAdapterTests extends ActiveMQMultiContextTests {

	@Autowired
	private PollableChannel out;

	@Test
	public void testTransactionalReceive() {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.convertAndSend("incatQ", "bar");
		assertNotNull(out.receive(20000));
		/*
		 *  INT-3288 - previously acknowledge="transacted"
		 *  Caused by: javax.jms.JMSException: acknowledgeMode SESSION_TRANSACTED cannot be used for an non-transacted Session
		 */
	}

	@Configuration
	@ImportResource("org/springframework/integration/jms/JmsInboundChannelAdapterTests-context.xml")
	public static class CFConfig {

		@Bean
		public ConnectionFactory connectionFactory() {
			return amqFactory;
		}
	}

}
