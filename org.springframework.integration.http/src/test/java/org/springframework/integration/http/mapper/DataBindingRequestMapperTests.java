/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.http.mapper;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.core.Message;
import org.springframework.integration.http.DataBindingRequestMapper;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.bind.support.ConfigurableWebBindingInitializer;

/**
 * @author Mark Fisher
 */
public class DataBindingRequestMapperTests {

	@Test
	public void bindToType() throws Exception {
		DataBindingRequestMapper mapper = new DataBindingRequestMapper(TestBean.class);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("name", "testBean");
		request.setParameter("age", "42");
		Message<?> result = mapper.mapRequest(request);
		assertNotNull(result);
		assertEquals(TestBean.class, result.getPayload().getClass());
		TestBean payload = (TestBean) result.getPayload();
		assertEquals("testBean", payload.name);
		assertEquals(84, payload.age);
	}

	@Test
	public void bindToTypeWithBindingInitializer() throws Exception {
		DataBindingRequestMapper mapper = new DataBindingRequestMapper(TestBean.class);
		ConfigurableWebBindingInitializer initializer = new ConfigurableWebBindingInitializer();
		initializer.setDirectFieldAccess(true);
		mapper.setWebBindingInitializer(initializer);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("name", "testBean");
		request.setParameter("age", "42");
		Message<?> result = mapper.mapRequest(request);
		assertNotNull(result);
		assertEquals(TestBean.class, result.getPayload().getClass());
		TestBean payload = (TestBean) result.getPayload();
		assertEquals("testBean", payload.name);
		assertEquals(42, payload.age);
	}

	@Test
	public void bindToPrototypeBean() throws Exception {
		StaticApplicationContext context = new StaticApplicationContext();
		MutablePropertyValues properties = new MutablePropertyValues();
		properties.addPropertyValue("name", "prototype");
		context.registerPrototype("prototypeTarget", TestBean.class, properties);
		DataBindingRequestMapper mapper = new DataBindingRequestMapper(TestBean.class);
		mapper.setTargetBeanName("prototypeTarget");
		mapper.setBeanFactory(context);
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setParameter("age", "42");
		Message<?> result = mapper.mapRequest(request);
		assertNotNull(result);
		assertEquals(TestBean.class, result.getPayload().getClass());
		TestBean payload = (TestBean) result.getPayload();
		assertEquals("prototype", payload.name);
		assertEquals(84, payload.age);
	}


	static class TestBean {

		String name;

		int age;

		public void setName(String name) {
			this.name = name;
		}

		public void setAge(int age) {
			this.age = age * 2;
		}
	}

}
