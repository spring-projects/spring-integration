/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
public class DynamicExpressionFilterIntegrationTests {

	@Autowired
	private MessageChannel input;

	@Autowired
	private PollableChannel positives;

	@Autowired
	private PollableChannel negatives;


	@Test
	void simpleExpressionBasedFilter() {
		this.input.send(new GenericMessage<>(1));
		this.input.send(new GenericMessage<>(0));
		this.input.send(new GenericMessage<>(99));
		this.input.send(new GenericMessage<>(-99));
		assertThat(positives.receive(0).getPayload()).isEqualTo(1);
		assertThat(positives.receive(0).getPayload()).isEqualTo(99);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(0);
		assertThat(negatives.receive(0).getPayload()).isEqualTo(-99);
		assertThat(positives.receive(0)).isNull();
		assertThat(negatives.receive(0)).isNull();
	}

}
