/*
 * Copyright 2016-2022 the original author or authors.
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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class StreamTransformerParserTests {

	@Autowired
	@Qualifier("directInput")
	private MessageChannel directInput;

	@Autowired
	@Qualifier("charsetChannel")
	private MessageChannel charsetChannel;

	@Autowired
	@Qualifier("queueInput")
	private MessageChannel queueInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test
	public void directChannelWithStringMessage() {
		this.directInput.send(new GenericMessage<InputStream>(new ByteArrayInputStream("foo".getBytes())));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat((byte[]) result.getPayload()).isEqualTo("foo".getBytes());
	}

	@Test
	public void queueChannelWithStringMessage() {
		this.queueInput.send(new GenericMessage<InputStream>(new ByteArrayInputStream("foo".getBytes())));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat((byte[]) result.getPayload()).isEqualTo("foo".getBytes());
	}

	@Test
	public void charset() {
		this.charsetChannel.send(new GenericMessage<InputStream>(new ByteArrayInputStream("foo".getBytes())));
		Message<?> result = output.receive(10000);
		assertThat(result).isNotNull();
		assertThat(result.getPayload()).isEqualTo("foo");
	}

}
