/*
 * Copyright 2002-2010 the original author or authors.
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

import java.io.IOException;

import javax.xml.transform.Source;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.oxm.Unmarshaller;
import org.springframework.oxm.XmlMappingException;
import org.springframework.xml.transform.StringSource;

/**
 * @author Jonas Partner
 * @author Mark Fisher
 */
public class UnmarshallingTransformerTests {

	@Test
	public void testStringSourceToString() {
		Unmarshaller unmarshaller = new TestUnmarshaller(false);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Object transformed = transformer.transformPayload(new StringSource("world"));
		assertEquals(String.class, transformed.getClass());
		assertEquals("hello world", transformed.toString());
	}

	@Test
	public void testMessageReturnValue() {
		Unmarshaller unmarshaller = new TestUnmarshaller(true);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Object transformed = transformer.transformPayload(new StringSource("foo"));
		assertEquals(GenericMessage.class, transformed.getClass());
		assertEquals("message: foo", ((Message<?>) transformed).getPayload());
	}

	@Test
	public void testMessageReturnValueFromTopLevel() {
		Unmarshaller unmarshaller = new TestUnmarshaller(true);
		UnmarshallingTransformer transformer = new UnmarshallingTransformer(unmarshaller);
		Message<?> result = transformer.transform(MessageBuilder.withPayload(new StringSource("bar")).build());
		assertNotNull(result);
		assertEquals("message: bar", result.getPayload());
	}


	private static class TestUnmarshaller implements Unmarshaller {

		private final boolean returnMessage;

		TestUnmarshaller(boolean returnMessage) {
			this.returnMessage = returnMessage;
		}

		public Object unmarshal(Source source) throws XmlMappingException, IOException {
			if (source instanceof StringSource) {
				char[] chars = new char[8];
				((StringSource) source).getReader().read(chars);
				if (returnMessage) {
					return new GenericMessage<String>("message: " + new String(chars).trim());
				}
				return "hello " + new String(chars).trim();
			}
			return null;
		}

		public boolean supports(Class<?> clazz) {
			return true;
		}
	}

}
