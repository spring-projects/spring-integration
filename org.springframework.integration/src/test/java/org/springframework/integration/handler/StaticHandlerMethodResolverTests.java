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
import java.util.Map;

import org.junit.Test;

import org.springframework.integration.annotation.Header;
import org.springframework.integration.annotation.Headers;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class StaticHandlerMethodResolverTests {

	@Test(expected = IllegalArgumentException.class)
	public void methodDeclaredOnObjectIsNotValid() throws Exception {
		Method method = Object.class.getDeclaredMethod("equals", new Class<?>[] {Object.class});
		new StaticHandlerMethodResolver(method);
	}

	@Test
	public void noArgMethodIsNotValid() throws Exception {
		Method method = TestBean.class.getDeclaredMethod("noArgMethod", new Class<?>[0]);
		new StaticHandlerMethodResolver(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void privateMethodIsNotValid() throws Exception {
		Method method = TestBean.class.getDeclaredMethod("privateMethod", new Class<?>[] {String.class});
		new StaticHandlerMethodResolver(method);
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullMethodIsNotValid() throws Exception {
		new StaticHandlerMethodResolver(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void ambiguousMethodIsNotValid() throws Exception {
		Class<?>[] types = new Class<?>[] { String.class, String.class };
		Method method = TestBean.class.getDeclaredMethod("ambiguousMethod", types);
		new StaticHandlerMethodResolver(method);
	}

	@Test
	public void validPayloadMethod() throws Exception {
		Method method = TestBean.class.getDeclaredMethod("payloadMethod", new Class<?>[] {String.class});
		HandlerMethodResolver resolver = new StaticHandlerMethodResolver(method);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("foo"));
		assertEquals(method, resolved);
	}

	@Test
	public void validHeaderMethod() throws Exception {
		Method method = TestBean.class.getDeclaredMethod("headerMethod", new Class<?>[] {String.class});
		HandlerMethodResolver resolver = new StaticHandlerMethodResolver(method);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("foo"));
		assertEquals(method, resolved);
	}

	@Test
	public void validHeaderMapMethod() throws Exception {
		Method method = TestBean.class.getDeclaredMethod("headerMapMethod", new Class<?>[] {Map.class});
		HandlerMethodResolver resolver = new StaticHandlerMethodResolver(method);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("foo"));
		assertEquals(method, resolved);
	}

	@Test
	public void validPayloadAndHeaderMethod() throws Exception {
		Class<?>[] types = new Class<?>[] { String.class, String.class };
		Method method = TestBean.class.getDeclaredMethod("payloadAndHeaderMethod", types);
		HandlerMethodResolver resolver = new StaticHandlerMethodResolver(method);
		Method resolved = resolver.resolveHandlerMethod(new StringMessage("foo"));
		assertEquals(method, resolved);
	}


	public static class TestBean {

		public void noArgMethod() {
		}

		@SuppressWarnings("unused")
		private void privateMethod(String s) {
		}

		public void ambiguousMethod(String s1, String s2) {
		}

		public void payloadMethod(String s) {
		}

		public void headerMethod(@Header("test") String s) {
		}

		public void headerMapMethod(@Headers Map<String, Object> headerMap) {
		}

		public void payloadAndHeaderMethod(String s1, @Header("test") String s2) {
		}
	}

}
