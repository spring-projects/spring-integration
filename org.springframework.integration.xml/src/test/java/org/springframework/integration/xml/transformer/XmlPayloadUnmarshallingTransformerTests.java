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

import javax.xml.transform.Source;

import org.junit.Test;

import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.message.Message;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 */
public class XmlPayloadUnmarshallingTransformerTests {

	@Test
	public void testStringSourceToString() {
		Unmarshaller unmarshaller = new TestUnmarshaller();
		XmlPayloadUnmarshallingTransformer transformer = new XmlPayloadUnmarshallingTransformer(unmarshaller);
		Message<?> message = new GenericMessage<StringSource>(new StringSource("world"));
		Message<?> transformed = transformer.transform(message);
		assertEquals(String.class, transformed.getPayload().getClass());
		assertEquals("hello world", transformed.getPayload().toString());
	}


	private static class TestUnmarshaller implements Unmarshaller {
		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			if (source instanceof StringSource) {
				char[] chars = new char[8];
				((StringSource) source).getReader().read(chars);
				return "hello " + new String(chars).trim();
			}
			return null;
		}

		@SuppressWarnings("unchecked")
		public boolean supports(Class clazz) {
			return true;
		}
	}

}
