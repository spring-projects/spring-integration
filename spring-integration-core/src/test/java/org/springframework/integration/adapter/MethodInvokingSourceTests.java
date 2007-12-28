/*
 * Copyright 2002-2007 the original author or authors.
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

import java.util.Collection;

import org.junit.Test;

import org.springframework.integration.MessageDeliveryException;

/**
 * @author Mark Fisher
 */
public class MethodInvokingSourceTests {

	@Test
	public void testSingleObjectResult() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("foo");
		Collection<Object> result = source.poll(5);
		assertNotNull(result);
		assertEquals("bar", result.iterator().next());
	}

	@Test(expected=MessageDeliveryException.class)
	public void testInvalidMethod() {
		MethodInvokingSource<TestBean> source = new MethodInvokingSource<TestBean>();
		source.setObject(new TestBean());
		source.setMethod("boo");
		source.poll(5);
	}


	private static class TestBean {

		public String foo() {
			return "bar";
		}
	}

}
