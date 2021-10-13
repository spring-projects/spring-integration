/*
 * Copyright 2015-2021 the original author or authors.
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

package org.springframework.integration.jms.request_reply;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class StopStartTests extends ActiveMQMultiContextTests {

	@Autowired
	@Qualifier("out")
	private Lifecycle outGateway;

	@Autowired
	private MessageChannel test;

	@Test
	public void test() {
		MessagingTemplate template = new MessagingTemplate(this.test);
		this.outGateway.start();
		assertThat(template.convertSendAndReceive("foo", String.class)).isEqualTo("FOO");
		this.outGateway.stop();
		this.outGateway.start();
		assertThat(template.convertSendAndReceive("bar", String.class)).isEqualTo("BAR");
		this.outGateway.stop();
	}

}
