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

package org.springframework.integration.filter;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 */
public class MethodInvokingSelectorTests {

	@Test
	public void acceptedWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "acceptString");
		assertTrue(selector.accept(new GenericMessage<String>("should accept")));
	}

	@Test
	public void acceptedWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("acceptString", new Class<?>[] { Message.class });
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		assertTrue(selector.accept(new GenericMessage<String>("should accept")));
	}

	@Test
	public void rejectedWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "acceptString");
		assertFalse(selector.accept(new GenericMessage<Integer>(99)));
	}

	@Test
	public void rejectedWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("acceptString", new Class<?>[] { Message.class });
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		assertFalse(selector.accept(new GenericMessage<Integer>(99)));
	}

	@Test
	public void noArgMethodWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "noArgs");
		selector.accept(new GenericMessage<String>("test"));
	}

	@Test
	public void noArgMethodWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("noArgs", new Class<?>[] {});
		new MethodInvokingSelector(testBean, method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturningMethodWithMethodName() {
		MethodInvokingSelector selector = new MethodInvokingSelector(new TestBean(), "returnVoid");
		selector.accept(new GenericMessage<String>("test"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void voidReturningMethodWithMethodReference() throws Exception {
		TestBean testBean = new TestBean();
		Method method = testBean.getClass().getMethod("returnVoid", new Class<?>[] { Message.class });
		MethodInvokingSelector selector = new MethodInvokingSelector(testBean, method);
		selector.accept(new GenericMessage<String>("test"));
	}


	@SuppressWarnings("unused")
	private static class TestBean {

		public boolean acceptString(Message<?> message) {
			return (message.getPayload() instanceof String);
		}

		public void returnVoid(Message<?> message) {
		}

		public boolean noArgs() {
			return true;
		}
	}

}
