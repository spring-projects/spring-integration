/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
public class InnerBeanConfigTests {

	@Autowired
	private EventDrivenConsumer testEndpoint;

	@Autowired
	private ApplicationContext context;

	@Test
	public void checkInnerBean() {
		Object innerBean = TestUtils.getPropertyValue(testEndpoint, "handler.processor.delegate.targetObject");
		assertThat(innerBean).isNotNull();
		assertThatExceptionOfType(NoSuchBeanDefinitionException.class)
				.isThrownBy(() -> context.getBean(TestBean.class));
	}

	public static class TestBean {

		public String echo(String value) {
			return value;
		}

	}

}
