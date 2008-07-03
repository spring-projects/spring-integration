/*
 * Copyright 2002-2008 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.xml.transform.Result;
import javax.xml.transform.dom.DOMResult;

import org.junit.Test;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;
import org.springframework.integration.xml.result.StringResultFactory;
import org.springframework.oxm.Marshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringResult;

/**
 * @author Mark Fisher
 */
public class XmlPayloadMarshallingTransformerTests {

	@Test
	public void testStringToStringResult() {
		TestMarshaller marshaller = new TestMarshaller();
		XmlPayloadMarshallingTransformer transformer = new XmlPayloadMarshallingTransformer(marshaller);
		transformer.setResultFactory(new StringResultFactory());
		Message<?> message = new StringMessage("world");
		transformer.transform(message);
		assertEquals(StringResult.class, message.getPayload().getClass());
		assertEquals("hello world", message.getPayload().toString());
		assertEquals("world", marshaller.payloads.get(0));
	}

	@Test
	public void testDefaultResultFactory() {
		TestMarshaller marshaller = new TestMarshaller();
		XmlPayloadMarshallingTransformer transformer = new XmlPayloadMarshallingTransformer(marshaller);
		Message<?> message = new StringMessage("world");
		transformer.transform(message);
		assertEquals(DOMResult.class, message.getPayload().getClass());
		assertEquals("world", marshaller.payloads.get(0));
	}

	private static class TestMarshaller implements Marshaller {

		List<Object> payloads = new ArrayList<Object>();

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}

		public void marshal(Object originalPayload, Result result) throws XmlMappingException, IOException {
			payloads.add(originalPayload);
			if (result instanceof StringResult) {
				((StringResult) result).getWriter().write("hello world");
			}

		}

	}

}
