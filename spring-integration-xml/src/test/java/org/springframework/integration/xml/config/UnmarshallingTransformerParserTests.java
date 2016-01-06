/*
 * Copyright 2002-2016 the original author or authors.
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

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.List;

import javax.xml.transform.dom.DOMSource;

import org.junit.Before;
import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.xml.util.XmlTestUtil;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.MultiValueMap;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 * @author Gary Russell
 */
public class UnmarshallingTransformerParserTests {

	private ApplicationContext appContext;

	private StubUnmarshaller unmarshaller;


	@Before
	public void setUp() {
		appContext = new ClassPathXmlApplicationContext(
				"UnmarshallingTransformerParserTests-context.xml", this.getClass());
		unmarshaller = (StubUnmarshaller) appContext.getBean("unmarshaller");
	}


	@Test
	public void testParse() throws Exception {
		EventDrivenConsumer consumer = (EventDrivenConsumer) appContext.getBean("parseOnly");
		assertEquals(2, TestUtils.getPropertyValue(consumer, "handler.order"));
		assertEquals(123L, TestUtils.getPropertyValue(consumer, "handler.messagingTemplate.sendTimeout"));
		assertEquals(-1, TestUtils.getPropertyValue(consumer, "phase"));
		assertFalse(TestUtils.getPropertyValue(consumer, "autoStartup", Boolean.class));
		SmartLifecycleRoleController roleController = appContext.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		List<SmartLifecycle> list = (List<SmartLifecycle>) TestUtils.getPropertyValue(roleController, "lifecycles",
				MultiValueMap.class).get("foo");
		assertThat(list, contains((SmartLifecycle) consumer));
	}

	@Test
	public void testDefaultUnmarshall() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<Object>(new StringSource(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		input.send(message);
		Message<?> result = output.receive(0);
		assertEquals("Wrong payload after unmarshalling", "unmarshalled", result.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof StringSource);
	}

	@Test
	public void testUnmarshallString() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<Object>(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>");
		input.send(message);
		Message<?> result = output.receive(0);
		assertEquals("Wrong payload after unmarshalling", "unmarshalled", result.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof StringSource);
	}

	@Test
	public void testUnmarshallDocument() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<Object>(
				XmlTestUtil.getDocumentForString("<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		input.send(message);
		Message<?> result = output.receive(0);
		assertEquals("Wrong payload after unmarshalling", "unmarshalled", result.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof DOMSource);
	}

	@Test
	public void testPollingUnmarshall() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("pollableInput");
		PollableChannel output = (PollableChannel) appContext.getBean("output");
		GenericMessage<Object> message = new GenericMessage<Object>(new StringSource(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		input.send(message);
		Message<?> result = output.receive(5000);
		assertEquals("Wrong payload after unmarshalling", "unmarshalled", result.getPayload());
		assertTrue("Wrong source passed to unmarshaller", unmarshaller.sourcesPassed.poll() instanceof StringSource);
	}




	@Test(expected = MessagingException.class)
	public void testUnmarshallUnsupported() throws Exception {
		MessageChannel input = (MessageChannel) appContext.getBean("input");
		GenericMessage<Object> message = new GenericMessage<Object>(new StringBuffer(
				"<?xml version=\"1.0\" encoding=\"ISO-8859-1\"?><order><orderItem>test</orderItem></order>"));
		input.send(message);
	}

}
