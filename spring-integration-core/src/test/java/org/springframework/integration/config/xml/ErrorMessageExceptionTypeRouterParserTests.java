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

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ErrorMessageExceptionTypeRouterParserTests {

	@Autowired
	private MessageChannel inputChannel;

	@Autowired
	private QueueChannel defaultChannel;

	@Autowired
	private QueueChannel illegalChannel;

	@Autowired
	private QueueChannel npeChannel;

	@Test
	public void validateExceptionTypeRouterConfig() {

		inputChannel.send(new ErrorMessage(new NullPointerException()));
		assertThat(npeChannel.receive(1000).getPayload()).isInstanceOf(NullPointerException.class);

		inputChannel.send(new ErrorMessage(new IllegalArgumentException()));
		assertThat(illegalChannel.receive(1000).getPayload()).isInstanceOf(IllegalArgumentException.class);

		inputChannel.send(new ErrorMessage(new RuntimeException()));
		assertThat(defaultChannel.receive(1000).getPayload()).isInstanceOf(RuntimeException.class);
	}

}
