/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.gateway;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Gunnar Hillert
 * @author Artem Bilan
 *
 * @since 2.0
 */
@SpringJUnitConfig
@DirtiesContext
public class MultiMethodGatewayConfigTests {

	@Autowired
	private ApplicationContext applicationContext;

	@Test
	public void validateGatewayMethods() {
		TestGateway gateway = this.applicationContext.getBean("myGateway", TestGateway.class);
		String parentClassName = "org.springframework.integration.gateway.MultiMethodGatewayConfigTests";
		assertThat(parentClassName + "$TestBeanA:oleg").isEqualTo(gateway.echo("oleg"));
		assertThat(parentClassName + "$TestBeanB:oleg").isEqualTo(gateway.echoUpperCase("oleg"));
		assertThat(parentClassName + "$TestBeanC:oleg").isEqualTo(gateway.echoViaDefault("oleg"));
	}

	public static class TestBeanA {

		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}

	}

	public static class TestBeanB {

		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}

	}

	public static class TestBeanC {

		public String echo(String str) {
			return this.getClass().getName() + ":" + str;
		}

	}

	public interface TestGateway {

		String echo(String str);

		String echoViaDefault(String str);

		String echoUpperCase(String str);

	}

}
