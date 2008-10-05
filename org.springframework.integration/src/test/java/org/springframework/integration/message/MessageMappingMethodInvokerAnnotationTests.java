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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class MessageMappingMethodInvokerAnnotationTests {

	@Test
	public void singleAnnotationMatches() {
		SingleAnnotationTestBean testBean = new SingleAnnotationTestBean();
		MessageMappingMethodInvoker invoker =
				new MessageMappingMethodInvoker(testBean, TestAnnotation.class);
		invoker.afterPropertiesSet();
		String result = (String) invoker.invokeMethod("foo");
		assertEquals("FOO", result);
	}

	@Test(expected = IllegalArgumentException.class)
	public void multipleAnnotationMatches() {
		MultipleAnnotationTestBean testBean = new MultipleAnnotationTestBean();
		MessageMappingMethodInvoker invoker =
				new MessageMappingMethodInvoker(testBean, TestAnnotation.class);
		invoker.afterPropertiesSet();
	}


	@Target(ElementType.METHOD)
	@Retention(RetentionPolicy.RUNTIME)
	private static @interface TestAnnotation {
	}


	private static class SingleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}
	}


	private static class MultipleAnnotationTestBean {

		@TestAnnotation
		public String upperCase(String s) {
			return s.toUpperCase();
		}

		@TestAnnotation
		public String lowerCase(String s) {
			return s.toLowerCase();
		}
	}

}
