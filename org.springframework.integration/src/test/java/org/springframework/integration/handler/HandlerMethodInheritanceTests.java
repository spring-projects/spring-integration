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

package org.springframework.integration.handler;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;

import org.junit.Test;

import org.springframework.integration.message.StringMessage;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 */
public class HandlerMethodInheritanceTests {

	@Test // INT-506
	public void overriddenMethodExcludedFromCandidateList() {
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(new TestSubclass());
		assertEquals(1, candidates.length);
		Method expected = ReflectionUtils.findMethod(
				TestSubclass.class, "test", new Class<?>[] { String.class });
		assertEquals(expected, candidates[0]);
	}

	@Test // INT-506
	public void overridingMethodResolves() {
		Method[] candidates = HandlerMethodUtils.getCandidateHandlerMethods(new TestSubclass());
		PayloadTypeMatchingHandlerMethodResolver resolver = new PayloadTypeMatchingHandlerMethodResolver(candidates);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("test"));
		Method expected = ReflectionUtils.findMethod(
				TestSubclass.class, "test", new Class<?>[] { String.class });
		assertEquals(expected, resolved);
	}


	public static class TestSuperclass {

		public void test(String s) {
		}
	}


	public static class TestSubclass extends TestSuperclass {

		public void test(String s) {
		}
	}

}
