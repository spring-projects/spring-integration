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

package org.springframework.integration.xml.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.test.context.ContextConfiguration;
import org.w3c.dom.Document;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
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
	@Test(expected=MessageRejectedException.class)
	public void testInvalidMessageWithThrowException() throws Exception {
		ApplicationContext ac = new ClassPathXmlApplicationContext("XmlPayloadValidatingFilterParserTests-context.xml", this.getClass());
		Document doc = XmlTestUtil.getDocumentForString("<greeting><other/></greeting>");
		GenericMessage<Document> docMessage = new GenericMessage<Document>(doc);
		PollableChannel validChannel = ac.getBean("validOutputChannel", PollableChannel.class);
		PollableChannel invalidChannel = ac.getBean("invalidOutputChannel", PollableChannel.class);
		MessageChannel inputChannel = ac.getBean("inputChannelB", MessageChannel.class);
		inputChannel.send(docMessage);
		assertNotNull(invalidChannel.receive(100));
		assertNull(validChannel.receive(100));
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
