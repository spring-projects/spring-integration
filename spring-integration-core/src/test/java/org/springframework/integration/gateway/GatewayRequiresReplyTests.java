/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class GatewayRequiresReplyTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void replyReceived() {
		TestService gateway = this.applicationContext.getBean("gateway", TestService.class);
		String result = gateway.test("foo");
		assertThat(result).isEqualTo("bar");
	}

	@Test
	public void noReplyReceived() {
		TestService gateway = this.applicationContext.getBean("gateway", TestService.class);
		assertThatExceptionOfType(ReplyRequiredException.class)
				.isThrownBy(() -> gateway.test("bad"));
	}

	@Test
	public void timedOutGateway() {
		TestService gateway = this.applicationContext.getBean("timeoutGateway", TestService.class);
		String result = gateway.test("hello");
		assertThat(result).isNull();
	}

	public interface TestService {

		String test(String s);

	}

	public static class LongRunningService {

		public String echo(String value) throws Exception {
			Thread.sleep(5000);
			return value;
		}

	}

}
