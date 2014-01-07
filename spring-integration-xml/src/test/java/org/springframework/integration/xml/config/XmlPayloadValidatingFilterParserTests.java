/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.xml.AggregatedXmlMessageValidationException;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
@ContextConfiguration
public class XmlPayloadValidatingFilterParserTests {

	@Test
	public void testValidMessage() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting>hello</greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		PollableChannel validChannel = ac.getBean("validOutputChannel", PollableChannel.class);
		MessageChannel inputChannel = ac.getBean("inputChannelA", MessageChannel.class);
		inputChannel.send(docMessage);
		assertNotNull(validChannel.receive(100));
	}
	@Test
	public void testInvalidMessageWithDiscardChannel() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting><other/></greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		PollableChannel validChannel = ac.getBean("validOutputChannel", PollableChannel.class);
		PollableChannel invalidChannel = ac.getBean("invalidOutputChannel", PollableChannel.class);
		MessageChannel inputChannel = ac.getBean("inputChannelA", MessageChannel.class);
		inputChannel.send(docMessage);
		assertNotNull(invalidChannel.receive(100));
		assertNull(validChannel.receive(100));
	}
	@Test
	public void testInvalidMessageWithThrowException() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting ping=\"pong\"><other/></greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		MessageChannel inputChannel = ac.getBean("inputChannelB", MessageChannel.class);
		try {
			inputChannel.send(docMessage);
			fail("MessageRejectedException expected");
		}
		catch (Exception e) {
			assertThat(e, Matchers.instanceOf(MessageRejectedException.class));
			Throwable cause = e.getCause();
			assertThat(cause, Matchers.instanceOf(AggregatedXmlMessageValidationException.class));
			assertThat(cause.getMessage(),
					Matchers.containsString("Element 'greeting' is a simple type, so it must have no element information item [children]."));
			assertThat(cause.getMessage(),
					Matchers.containsString("Element 'greeting' is a simple type, so it cannot have attributes,"));
		}
	}
	@Test
	public void testValidMessageWithValidator() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting>hello</greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		PollableChannel validChannel = ac.getBean("validOutputChannel", PollableChannel.class);
		MessageChannel inputChannel = ac.getBean("inputChannelC", MessageChannel.class);
		inputChannel.send(docMessage);
		assertNotNull(validChannel.receive(100));
	}
	@SuppressWarnings("unused")
	@Test
	public void testInvalidMessageWithValidatorAndDiscardChannel() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting><other/></greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		PollableChannel validChannel = ac.getBean("validOutputChannel", PollableChannel.class);
		PollableChannel invalidChannel = ac.getBean("invalidOutputChannel", PollableChannel.class);
		MessageChannel inputChannel = ac.getBean("inputChannelC", MessageChannel.class);
		inputChannel.send(docMessage);
		assertNotNull(invalidChannel.receive(100));
	}
}
