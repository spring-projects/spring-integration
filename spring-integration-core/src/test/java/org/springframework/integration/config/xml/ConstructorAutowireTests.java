/*
 * Copyright 2002-present the original author or authors.
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
