/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.ip.config;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.1.1
 */
public class TcpConnectionFactoryFactoryBeanTest {

	@Test
	public void testNoReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.afterPropertiesSet();
		// INT-3578 IllegalArgumentException on 'readDelay'
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(100L);
	}

	@Test
	public void testReadDelay() throws Exception {
		TcpConnectionFactoryFactoryBean fb = new TcpConnectionFactoryFactoryBean();
		fb.setHost("foo");
		fb.setReadDelay(1000);
		fb.setBeanFactory(mock(BeanFactory.class));
		fb.afterPropertiesSet();
		assertThat(TestUtils.getPropertyValue(fb.getObject(), "readDelay")).isEqualTo(1000L);
	}

}
