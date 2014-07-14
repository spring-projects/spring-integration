/*
 * Copyright 2002-2009 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import javax.xml.transform.dom.DOMResult;

import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.xml.config.StubResultFactory.StubStringResult;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class MarshallingTransformerParserTests  {

	private ApplicationContext appContext;

	private PollableChannel output;


	@Before
	public void setUp() {
		this.appContext = new ClassPathXmlApplicationContext("MarshallingTransformerParserTests-context.xml", getClass());
		this.output = (PollableChannel) appContext.getBean("output");
	}


	@Test
	public void testDefault() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerNoResultFactory");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "hello", doc.getDocumentElement().getTextContent());
	}

	@Test
	public void testDefaultWithResultTransformer() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerWithResultTransformer");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type", result.getPayload() instanceof String);
		String resultPayload = (String)result.getPayload();
		assertEquals("Wrong payload", "testReturn", resultPayload);
	}

	@Test
	public void testDOMResult() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerDOMResultFactory");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type ", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "hello", doc.getDocumentElement().getTextContent());
	}

	@Test
	public void testStringResult() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerStringResultFactory");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type", result.getPayload() instanceof StringResult);
	}

	@Test
	public void testCustomResultFactory() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerCustomResultFactory");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type", result.getPayload() instanceof StubStringResult);
	}

	@Test
	public void testFullMessage() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("marshallingTransformerWithFullMessage");
		GenericMessage<Object> message = new GenericMessage<Object>("hello");
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Wrong payload type", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		String actual = doc.getDocumentElement().getTextContent();
		assertThat(actual, Matchers.containsString("[Payload"));
		assertThat(actual, Matchers.containsString("=hello]"));
		assertThat(actual, Matchers.containsString("[Headers="));
	}

}
