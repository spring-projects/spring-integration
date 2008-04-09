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

package org.springframework.integration.adapter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.integration.message.MessagingException;

/**
 * @author Mark Fisher
 */
public class MethodInvokingSourceTests {

	@Test
	public void testValidMethod() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("validMethod");
		Object result = source.poll();
		assertNotNull(result);
		assertEquals("valid", result);
	}

	@Test(expected=MessagingException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("noMatchingMethod");
		source.poll();
	}

	@Test(expected=MessagingException.class)
	public void testInvalidMethodWithArg() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("invalidMethodWithArg");
		source.poll();
	}

	@Test(expected=MessagingException.class)
	public void testInvalidMethodWithNoReturnValue() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("invalidMethodWithNoReturnValue");
		source.poll();
	}


	private static class TestBean {

		public String validMethod() {
			return "valid";
		}

		public String invalidMethodWithArg(String arg) {
			return "invalid";
		}

		public void invalidMethodWithNoReturnValue() {
		}
	}

}
