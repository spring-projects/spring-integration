/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 1.0.3
 */
@SpringJUnitConfig
@DirtiesContext
public class OrderedHandlersTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyOrder() {
		for (int i = 1; i < 14; i++) {
			Object consumer = context.getBean("endpoint" + i);
			Object handler = new DirectFieldAccessor(consumer).getPropertyValue("handler");
			assertThat(handler instanceof Ordered).isTrue();
			assertThat(((Ordered) handler).getOrder()).isEqualTo(i);
		}
	}

	static class TestBean {

		public Object handle(Object o) {
			return o;
		}

		public boolean filter() {
			return true;
		}

	}

}
