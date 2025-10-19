/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.ip.tcp.outbound;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.BDDMockito.given;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class TcpSendingNoSocketTests {

	@Autowired
	private MessageChannel shouldFail;

	@Autowired
	private MessageChannel advised;

	@Autowired
	private AbstractServerConnectionFactory mockCf;

	@BeforeEach
	void setup() {
		given(mockCf.getApplicationEventPublisher()).willReturn(event -> {
		});
	}

	@Test
	public void exceptionExpected() {
		assertThatExceptionOfType(MessageHandlingException.class)
				.isThrownBy(() -> shouldFail.send(new GenericMessage<>("foo")))
				.withMessageStartingWith("Unable to find outbound socket");
	}

	@Test
	public void exceptionTrapped() {
		advised.send(new GenericMessage<>("foo"));
	}

}
