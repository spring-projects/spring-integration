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

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.message.MessageDeliveryException;

/**
 * @author Mark Fisher
 */
public class MethodInvokingTargetTests {

	@Test
	public void testValidMethod() {
		MethodInvokingTarget<TestSink> target = new MethodInvokingTarget<TestSink>();
		target.setObject(new TestSink());
		target.setMethod("validMethod");
		target.afterPropertiesSet();
		boolean result = target.send("test");
		assertTrue(result);
	}

	@Test(expected=MessageDeliveryException.class)
	public void testInvalidMethodWithNoArgs() {
		MethodInvokingTarget<TestSink> target = new MethodInvokingTarget<TestSink>();
		target.setObject(new TestSink());
		target.setMethod("invalidMethodWithNoArgs");
		target.afterPropertiesSet();
		target.send("test");
	}

	@Test
	public void testValidMethodWithIgnoredReturnValue() {
		MethodInvokingTarget<TestSink> target = new MethodInvokingTarget<TestSink>();
		target.setObject(new TestSink());
		target.setMethod("validMethodWithIgnoredReturnValue");
		target.afterPropertiesSet();
		boolean result = target.send("test");
		assertTrue(result);
	}

	@Test(expected=MessageDeliveryException.class)
	public void testNoMatchingMethodName() {
		MethodInvokingTarget<TestSink> target = new MethodInvokingTarget<TestSink>();
		target.setObject(new TestSink());
		target.setMethod("noSuchMethod");
		target.afterPropertiesSet();
		target.send("test");
	}

}
