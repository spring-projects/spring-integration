/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Jim Moore
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class ConstructorAutowireTests {

	@Autowired
	TestService service;

	@Autowired
	TestEndpoint testEndpoint;

	@Test
	public void testApplicationContextCreation() throws InterruptedException {
		assertThat(this.testEndpoint.consumerLatch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(this.testEndpoint.result).isEqualTo(this.service.getVal());
	}

	public static class TestService {

		public String getVal() {
			return "test data";
		}

	}

	public static class TestEndpoint {

		private CountDownLatch consumerLatch = new CountDownLatch(1);

		private String result;

		private TestService service;

		@Autowired
		public TestEndpoint(TestService service) {
			this.service = service;
		}

		public String aProducer() {
			return this.service.getVal();
		}

		public void aConsumer(String str) {
			this.result = str;
			this.consumerLatch.countDown();
		}

		public List<String> aSplitter(List<String> strs) {
			return strs;
		}

	}

}
