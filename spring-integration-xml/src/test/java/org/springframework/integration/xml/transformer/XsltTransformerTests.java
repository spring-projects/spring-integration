/*
 * Copyright 2002-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.xml.transformer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertFalse;

import java.io.IOException;
import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;

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
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Jonas Partner
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class XsltTransformerTests {
	private String docAsString = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";
	@Autowired
	private ApplicationContext applicationContext;
	@Autowired
	@Qualifier("output")
	private QueueChannel output;

	@Test
	public void testParamHeadersWithStartWildCharacter() {
		MessageChannel input = applicationContext.getBean("paramHeadersWithStartWildCharacterChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		MessageHistory history = MessageHistory.read(resultMessage);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "paramHeadersWithStartWildCharacter", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("xml:xslt-transformer", componentHistoryRecord.get("type"));
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		assertTrue(((String) resultMessage.getPayload()).contains("testParamValue"));
		assertFalse(((String) resultMessage.getPayload()).contains("FOO"));
	}

	@Test
	public void testParamHeadersWithEndWildCharacter() {
		MessageChannel input = applicationContext.getBean("paramHeadersWithEndWildCharacterChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		assertTrue(((String) resultMessage.getPayload()).contains("testParamValue"));
		assertTrue(((String) resultMessage.getPayload()).contains("FOO"));
	}

	@Test
	public void testParamHeadersWithIndividualParameters() {
		MessageChannel input = applicationContext.getBean("paramHeadersWithIndividualParametersChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		assertTrue(((String) resultMessage.getPayload()).contains("testParamValue"));
		assertTrue(((String) resultMessage.getPayload()).contains("FOO"));
		assertTrue(((String) resultMessage.getPayload()).contains("hello"));
	}

	@Test
	public void testParamHeadersCombo() {
		MessageChannel input = applicationContext.getBean("paramHeadersComboChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				setHeader("testParam", "testParamValue").
				setHeader("testParam2", "FOO").
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		assertTrue(((String) resultMessage.getPayload()).contains("testParamValue"));
		assertTrue(((String) resultMessage.getPayload()).contains("FOO"));
		assertTrue(((String) resultMessage.getPayload()).contains("hello"));
	}


	@Test
	public void outputAsString() {
		MessageChannel input = applicationContext.getBean("outputAsStringChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.docAsString).
				build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		String stringPayload = (String)resultMessage.getPayload();
		assertEquals("Wrong content of payload", "hello world text",stringPayload.trim());
	}

	@Test
	public void testInt3067OutputFileAsString() throws IOException {
		MessageChannel input = applicationContext.getBean("outputFileAsStringChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(new ClassPathResource("org/springframework/integration/xml/transformer/xsl-text-file.xml").getFile()).build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		assertEquals("Wrong payload type", String.class, resultMessage.getPayload().getClass());
		String stringPayload = (String) resultMessage.getPayload();
		assertEquals("Wrong content of payload", "hello world text", stringPayload.trim());
	}
}

