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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.xml.transform.dom.DOMResult;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.config.StubResultFactory.StubStringResult;
import org.springframework.integration.xml.transformer.CustomTestResultFactory;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.xml.transform.StringResult;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gunnar Hillert
 */
public class XsltPayloadTransformerParserTests {

	private final String doc = "<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>";

	private ApplicationContext applicationContext;

	private PollableChannel output;

	@Before
	public void setUp() {
		applicationContext = new ClassPathXmlApplicationContext(getClass().getSimpleName() + "-context.xml", getClass());
		output = (PollableChannel) applicationContext.getBean("output");
	}

	@Test
	public void testWithResourceProvided() throws Exception {
		MessageChannel input = (MessageChannel) applicationContext.getBean("withResourceIn");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Payload was not a DOMResult", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "test", doc.getDocumentElement().getTextContent());
		assertNotNull(TestUtils.getPropertyValue(applicationContext.getBean("xsltTransformerWithResource.handler"),
				"transformer.evaluationContext.beanResolver"));
	}

	@Test
	public void testWithTemplatesProvided() throws Exception {
		MessageChannel input = (MessageChannel) applicationContext.getBean("withTemplatesIn");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Payload was not a DOMResult", result.getPayload() instanceof DOMResult);
		Document doc = (Document) ((DOMResult) result.getPayload()).getNode();
		assertEquals("Wrong payload", "test", doc.getDocumentElement().getTextContent());
	}

	@Test
	public void testWithTemplatesAndResultTransformer() throws Exception {
		MessageChannel input = (MessageChannel) applicationContext.getBean("withTemplatesAndResultTransformerIn");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		input.send(message);
		Message<?> result = output.receive(0);
		assertEquals("Wrong payload type", String.class, result.getPayload().getClass());
		String strResult = (String) result.getPayload();
		assertEquals("Wrong payload", "testReturn", strResult);
	}

	@Test
	public void testWithResourceProvidedAndStubResultFactory() throws Exception {
		MessageChannel input = (MessageChannel) applicationContext.getBean("withTemplatesAndResultFactoryIn");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Payload was not a StubStringResult", result.getPayload() instanceof StubStringResult);
	}

	@Test
	public void testWithResourceAndStringResultType() throws Exception {
		MessageChannel input = (MessageChannel) applicationContext.getBean("withTemplatesAndStringResultTypeIn");
		GenericMessage<Object> message = new GenericMessage<Object>(XmlTestUtil.getDomSourceForString(doc));
		input.send(message);
		Message<?> result = output.receive(0);
		assertTrue("Payload was not a StringResult", result.getPayload() instanceof StringResult);
	}

	@Test
	public void docInStringResultOut() throws Exception {
		MessageChannel input = applicationContext.getBean("docinStringResultOutTransformerChannel",
				MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(XmlTestUtil.getDocumentForString(this.doc)).build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		Assert.assertEquals("Wrong payload type", StringResult.class, resultMessage.getPayload().getClass());
		String payload = resultMessage.getPayload().toString();
		Assert.assertTrue(payload.contains("<bob>test</bob>"));
	}

	@Test
	public void stringInDocResultOut() throws Exception {
		MessageChannel input = applicationContext.getBean("stringResultOutTransformerChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(this.doc).build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		Assert.assertEquals("Wrong payload type", DOMResult.class, resultMessage.getPayload().getClass());
		Document payload = (Document) ((DOMResult) resultMessage.getPayload()).getNode();
		Assert.assertTrue(XmlTestUtil.docToString(payload).contains("<bob>test</bob>"));
	}

	@Test
	public void stringInAndCustomResultFactory() throws Exception {
		MessageChannel input = applicationContext.getBean("stringInCustomResultFactoryChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload(XmlTestUtil.getDocumentForString(this.doc)).build();
		input.send(message);
		Message<?> resultMessage = output.receive();
		Assert.assertEquals("Wrong payload type", CustomTestResultFactory.FixedStringResult.class, resultMessage
				.getPayload().getClass());
		String payload = resultMessage.getPayload().toString();
		Assert.assertTrue(payload.contains("fixedStringForTesting"));
	}

}
