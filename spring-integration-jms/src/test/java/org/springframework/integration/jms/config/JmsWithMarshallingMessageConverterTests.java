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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;

import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class JmsWithMarshallingMessageConverterTests {

	@Test
	@SuppressWarnings("unchecked")
	public void demoWithMarshallingConverter() {
		ActiveMqTestUtils.prepare();
		ApplicationContext ac = new ClassPathXmlApplicationContext(
				"JmsWithMarshallingMessageConverterTests-context.xml", JmsWithMarshallingMessageConverterTests.class);
		MessageChannel input = ac.getBean("outbound-gateway-channel", MessageChannel.class);
		PollableChannel output = ac.getBean("output", PollableChannel.class);
		input.send(new GenericMessage<String>("hello"));
		Message<String> replyMessage = (Message<String>) output.receive();
		MessageHeaders headers = replyMessage.getHeaders();
		// check for couple of JMS headers, make sure they are present
		assertNotNull(headers.get("jms_redelivered"));
		assertEquals("HELLO", replyMessage.getPayload());
	}


	public static class SampleService {

		public String echo(String value) {
			return value.toUpperCase();
		}
	}


	public static class SampleMarshaller implements Marshaller {

		public void marshal(Object graph, Result result) throws IOException, XmlMappingException {
			String payload = null;
			if (graph instanceof Message<?>) {
				payload = (String) ((Message<?>)graph).getPayload();
			}
			else {
				payload = (String) graph;
			}
			((StreamResult)result).getOutputStream().write(payload.getBytes());
		}

		public boolean supports(Class<?> clazz) {
			return true;
		}
	}


	public static class SampleUnmarshaller implements Unmarshaller {

		public boolean supports(Class<?> clazz) {
			return true;
		}

		public Object unmarshal(Source source) throws IOException, XmlMappingException {
			InputStream io = ((StreamSource)source).getInputStream();
			byte[] bytes = new byte[io.available()];
			io.read(bytes);
			return new GenericMessage<String>(new String(bytes));
		}
	}

}
