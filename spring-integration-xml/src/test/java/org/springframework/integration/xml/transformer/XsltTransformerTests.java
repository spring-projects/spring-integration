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

package org.springframework.integration.xml.transformer;

import java.io.IOException;
import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ClassPathResource;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class XsltTransformerTests {

	private final String docAsString = """
			<?xml version="1.0" encoding="ISO-8859-1"?>
			<order>
				<orderItem>test</orderItem>
			</order>
			""";

	@Autowired
	private ApplicationContext applicationContext;

	@Autowired
	@Qualifier("output")
	private QueueChannel output;

	@Test
	public void testParamHeadersWithStartWildCharacter() {
		var input = applicationContext.getBean("paramHeadersWithStartWildCharacterChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		MessageHistory history = MessageHistory.read(resultMessage);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord =
				TestUtils.locateComponentInHistory(history, "paramHeadersWithStartWildCharacter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("xml:xslt-transformer");
		assertThat(resultMessage.getPayload())
				.as("Wrong payload type")
				.isInstanceOf(String.class)
				.asString()
				.contains("testParamValue")
				.doesNotContain("FOO");
	}

	@Test
	public void testParamHeadersWithEndWildCharacter() {
		var input = applicationContext.getBean("paramHeadersWithEndWildCharacterChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertThat(resultMessage.getPayload())
				.as("Wrong payload type")
				.isInstanceOf(String.class)
				.asString()
				.contains("testParamValue", "FOO");
	}

	@Test
	public void testParamHeadersWithIndividualParameters() {
		var input = applicationContext.getBean("paramHeadersWithIndividualParametersChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertThat(resultMessage.getPayload())
				.as("Wrong payload type")
				.isInstanceOf(String.class)
				.asString()
				.contains("testParamValue", "FOO", "hello");
	}

	@Test
	public void testParamHeadersCombo() {
		var input = applicationContext.getBean("paramHeadersComboChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertThat(resultMessage.getPayload())
				.as("Wrong payload type")
				.isInstanceOf(String.class)
				.asString()
				.contains("testParamValue", "FOO", "hello");
	}

	@Test
	public void outputAsString() {
		var input = applicationContext.getBean("outputAsStringChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertThat(resultMessage.getPayload().getClass()).as("Wrong payload type").isEqualTo(String.class);
		String stringPayload = (String) resultMessage.getPayload();
		assertThat(stringPayload.trim()).as("Wrong content of payload").isEqualTo("hello world text");
	}

	@Test
	public void testInt3067OutputFileAsString() throws IOException {
		var input = applicationContext.getBean("outputFileAsStringChannel", MessageChannel.class);
		Message<?> message =
				MessageBuilder.withPayload(
								new ClassPathResource(
										"org/springframework/integration/xml/transformer/xsl-text-file.xml")
										.getFile())
						.build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertThat(resultMessage.getPayload().getClass()).as("Wrong payload type").isEqualTo(String.class);
		String stringPayload = (String) resultMessage.getPayload();
		assertThat(stringPayload.trim()).as("Wrong content of payload").isEqualTo("hello world text");
	}

}
