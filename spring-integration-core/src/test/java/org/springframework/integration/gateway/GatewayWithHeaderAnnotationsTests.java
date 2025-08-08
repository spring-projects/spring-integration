/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewayWithHeaderAnnotationsTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void priorityAsArgument() {
		TestService gateway = (TestService) applicationContext.getBean("gateway");
		String result = gateway.test("foo", 99, "bar", "qux");
		assertThat(result).isEqualTo("foo99barqux");
	}

	public interface TestService {

		// wrt INT-1205, priority no longer has a $ prefix, so here we are testing the $custom header as well
		String test(String str, @Header(IntegrationMessageHeaderAccessor.PRIORITY) int priority,
				@Header("$custom") String custom, @Header(name = "baz") String baz);

	}

}
