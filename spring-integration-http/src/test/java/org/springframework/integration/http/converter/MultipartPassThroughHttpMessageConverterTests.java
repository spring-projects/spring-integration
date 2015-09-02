/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.http.converter;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;

import org.junit.Test;

import org.springframework.http.HttpInputMessage;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
public class MultipartPassThroughHttpMessageConverterTests {

	@Test
	public void testConvert() throws Exception {
		MultipartPassThroughHttpMessageConverter converter = new MultipartPassThroughHttpMessageConverter();
		HttpInputMessage inputMessage = mock(HttpInputMessage.class);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
		when(inputMessage.getBody()).thenReturn(bais);
		byte[] out = converter.read(byte[].class, inputMessage);
		assertEquals("foo", new String(out));
	}

}
