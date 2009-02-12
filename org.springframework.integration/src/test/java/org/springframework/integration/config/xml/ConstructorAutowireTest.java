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

package org.springframework.integration.config.xml;

import java.util.List;

import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * @author Jim Moore
 * @author Mark Fisher
 */
public class ConstructorAutowireTest {

	@Test // INT-568
	public void testApplicationContextCreation() {
		new ClassPathXmlApplicationContext("ConstructorAutowireTest-context.xml", ConstructorAutowireTest.class);
	}


	public static class TestService {
		public String getVal() {
			return "fooble";
		}
	}


	public static class TestEndpoint {

		private TestService service;

		@Autowired
		public TestEndpoint(TestService service) {
			this.service = service;
		}

		public String aProducer() {
			return this.service.getVal();
		}

		public void aConsumer(String str) {
			// ignore
		}

		public List<String> aSplitter(List<String> strs) {
			return strs;
		}
	}

}
