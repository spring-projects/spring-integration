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

package org.springframework.integration.transformer;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class MethodInvokingPayloadTransformerTests {

	@Test
	public void testSimpleMethod() throws Exception {
		MethodInvokingPayloadTransformer transformer = new MethodInvokingPayloadTransformer();
		transformer.setObject(new TestBean());
		transformer.setMethodName("exclaim");
		assertEquals("FOO!", transformer.transform("foo"));
	}

	@Test
	public void testTypeConversion() throws Exception {
		MethodInvokingPayloadTransformer transformer = new MethodInvokingPayloadTransformer();
		transformer.setObject(new TestBean());
		transformer.setMethodName("exclaim");
		assertEquals("123!", transformer.transform(123));
	}

	@Test(expected=NoSuchMethodException.class)
	public void testTypeConversionFailure() throws Exception {
		MethodInvokingPayloadTransformer transformer = new MethodInvokingPayloadTransformer();
		transformer.setObject(new TestBean());
		transformer.setMethodName("exclaim");
		transformer.transform(new Date());
	}


	private static class TestBean {

		String exclaim(String input) {
			return input.toUpperCase() + "!";
		}
	}

}
