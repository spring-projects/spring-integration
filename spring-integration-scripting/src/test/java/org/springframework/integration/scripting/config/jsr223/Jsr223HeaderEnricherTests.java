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

package org.springframework.integration.scripting.config.jsr223;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class Jsr223HeaderEnricherTests {

	@Autowired
	private MessageChannel inputA;

	@Autowired
	private QueueChannel outputA;

	@Autowired
	private MessageChannel inputB;

	@Autowired
	private QueueChannel outputB;

	@Test
	public void referencedScript() {
		inputA.send(new GenericMessage<>("Hello"));
		assertThat(outputA.receive(20000).getHeaders().get("TEST_HEADER")).isEqualTo("jruby");
	}

	@Test
	public void inlineScript() {
		inputB.send(new GenericMessage<>("Hello"));
		assertThat(outputB.receive(20000).getHeaders().get("TEST_HEADER")).isEqualTo("js");
	}

}
