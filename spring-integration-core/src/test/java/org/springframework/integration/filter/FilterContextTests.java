/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.filter;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class FilterContextTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint pojoFilter;

	@Autowired
	private TestBean testBean;

	@Test
	public void methodInvokingFilterRejects() {
		this.input.send(new GenericMessage<>("foo"));
		Message<?> reply = this.output.receive(0);
		assertThat(reply).isNull();

		assertThat(this.testBean.isRunning()).isTrue();
		this.pojoFilter.stop();
		assertThat(this.testBean.isRunning()).isFalse();
		this.pojoFilter.start();
		assertThat(this.testBean.isRunning()).isTrue();
	}

	@Test
	public void methodInvokingFilterAccepts() {
		this.input.send(new GenericMessage<>("foobar"));
		Message<?> reply = this.output.receive(0);
		assertThat(reply.getPayload()).isEqualTo("foobar");
	}

}
