/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author David Turanski
 * @author Artem Bilan
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class Jsr223RouterTests {

	@Autowired
	private MessageChannel referencedScriptInput;

	@Autowired
	private MessageChannel inlineScriptInput;

	@Autowired
	private MessageChannel scriptRouterWithinChainInput;

	@Autowired
	private PollableChannel longStrings;

	@Autowired
	private PollableChannel shortStrings;


	@Test
	public void referencedScript() { // long is > 3
		Message<?> message1 = new GenericMessage<String>("aardvark");
		Message<?> message2 = new GenericMessage<String>("bear");
		Message<?> message3 = new GenericMessage<String>("cat");
		Message<?> message4 = new GenericMessage<String>("dog");
		Message<?> message5 = new GenericMessage<String>("elephant");
		this.referencedScriptInput.send(message1);
		this.referencedScriptInput.send(message2);
		this.referencedScriptInput.send(message3);
		this.referencedScriptInput.send(message4);
		this.referencedScriptInput.send(message5);
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("cat");
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("dog");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("aardvark");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("bear");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("elephant");
		assertThat(shortStrings.receive(0)).isNull();
		assertThat(longStrings.receive(0)).isNull();
	}

	@Test
	public void inlineScript() { // long is > 5
		Message<?> message1 = new GenericMessage<String>("aardvark");
		Message<?> message2 = new GenericMessage<String>("bear");
		Message<?> message3 = new GenericMessage<String>("cat");
		Message<?> message4 = new GenericMessage<String>("dog");
		Message<?> message5 = new GenericMessage<String>("elephant");
		this.inlineScriptInput.send(message1);
		this.inlineScriptInput.send(message2);
		this.inlineScriptInput.send(message3);
		this.inlineScriptInput.send(message4);
		this.inlineScriptInput.send(message5);
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("bear");
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("cat");
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("dog");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("aardvark");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("elephant");
		assertThat(shortStrings.receive(0)).isNull();
		assertThat(longStrings.receive(0)).isNull();
	}

	@Test
	public void testInt2893ScriptRouterWithinChain() {
		Message<?> message1 = new GenericMessage<String>("aardvark");
		Message<?> message2 = new GenericMessage<String>("bear");
		Message<?> message3 = new GenericMessage<String>("cat");
		Message<?> message4 = new GenericMessage<String>("dog");
		Message<?> message5 = new GenericMessage<String>("elephant");
		this.scriptRouterWithinChainInput.send(message1);
		this.scriptRouterWithinChainInput.send(message2);
		this.scriptRouterWithinChainInput.send(message3);
		this.scriptRouterWithinChainInput.send(message4);
		this.scriptRouterWithinChainInput.send(message5);
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("bear");
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("cat");
		assertThat(shortStrings.receive(0).getPayload()).isEqualTo("dog");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("aardvark");
		assertThat(longStrings.receive(0).getPayload()).isEqualTo("elephant");
		assertThat(shortStrings.receive(0)).isNull();
		assertThat(longStrings.receive(0)).isNull();
	}

}
