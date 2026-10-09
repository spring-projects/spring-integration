/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.monitor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Gary Russell
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class ChainWithMessageProducingHandlersTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void testSuccessfulApplicationContext() {
		// this is all we need to do. Until INT-1431 was solved initialization of this AC would fail.
		assertThat(applicationContext).isNotNull();
	}

	public static class SampleProducer {

		public String echo(String value) {
			return value;
		}

	}

	public static class SampleService {

		public void echo(String value) {
		}

	}

}
