/*
 * Copyright 2014-2021 the original author or authors.
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

package org.springframework.integration.jms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.PollableChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class JmsInboundChannelAdapterTests extends ActiveMQMultiContextTests {

	@Autowired
	private PollableChannel out;

	@Test
	public void testTransactionalReceive() {
		JmsTemplate template = new JmsTemplate(connectionFactory);
		template.convertAndSend("incatQ", "bar");
		assertThat(out.receive(20000)).isNotNull();
		/*
		 *  INT-3288 - previously acknowledge="transacted"
		 *  Caused by: javax.jms.JMSException: acknowledgeMode SESSION_TRANSACTED cannot be used for an non-transacted Session
		 */
	}

}
