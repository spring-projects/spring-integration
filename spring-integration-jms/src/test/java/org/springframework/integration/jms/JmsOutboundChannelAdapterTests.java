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
import static org.junit.Assert.assertNull;

import javax.jms.ConnectionFactory;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.integration.jms.JmsOutboundChannelAdapterTests.CFConfig;
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
public class JmsOutboundChannelAdapterTests extends ActiveMQMultiContextTests {

	@Autowired
	private PollableChannel out;

	@Autowired
	private Aborter aborter;

	@Autowired
	private JmsMessageDrivenEndpoint endpoint;

	@Test
	public void testTransactionalSend() {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.convertAndSend("outcatQ1", "Hello, world!");
		template.setReceiveTimeout(20000);
		assertNotNull(template.receive("outcatQ2"));

		this.aborter.abort = true;
		template.convertAndSend("outcatQ1", "Hello, world!");
		template.setReceiveTimeout(1000);
		assertNull(template.receive("outcatQ2"));
		endpoint.stop();
	}

	@Configuration
	@ImportResource("org/springframework/integration/jms/JmsOutboundChannelAdapterTests-context.xml")
	public static class CFConfig {

		@Bean
		public ConnectionFactory connectionFactory() {
			return connectionFactory;
		}
	}

	public static class Aborter {

		private volatile boolean abort;

		public void foo() {
			if (abort) {
				throw new RuntimeException("intentional");
			}
		}
	}

}
